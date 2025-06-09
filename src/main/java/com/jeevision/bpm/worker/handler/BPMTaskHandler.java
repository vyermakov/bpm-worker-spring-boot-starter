package com.jeevision.bpm.worker.handler;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jeevision.bpm.worker.annotation.BPMError;
import com.jeevision.bpm.worker.annotation.BPMResult;
import com.jeevision.bpm.worker.model.WorkerMethod;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles execution of BPM worker methods.
 * Supports both BPMN errors and technical failures/incidents.
 *
 * @author Slava Yermakov
 * @email v.yermakov@gmail.com
 */
@Slf4j
@RequiredArgsConstructor
public class BPMTaskHandler implements ExternalTaskHandler {
    
    private final ObjectMapper objectMapper;
    private WorkerMethod workerMethod;
    
    public BPMTaskHandler withWorkerMethod(WorkerMethod workerMethod) {
        this.workerMethod = workerMethod;
        return this;
    }
    
    @Override
    public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        try {
            log.debug("Executing task {} for topic {}", externalTask.getId(), externalTask.getTopicName());
            
            var args = prepareMethodArguments(externalTask);
            var result = workerMethod.getMethod().invoke(workerMethod.getBean(), args);
            
            var variables = processResult(result);
            
            externalTaskService.complete(externalTask, variables);
            log.debug("Completed task {} for topic {}", externalTask.getId(), externalTask.getTopicName());
            
        } catch (Exception e) {
            handleException(externalTask, externalTaskService, e);
        }
    }
    
    private void handleException(ExternalTask externalTask, ExternalTaskService externalTaskService, Exception e) {
        var taskId = externalTask.getId();
        var topicName = externalTask.getTopicName();
        
        var actualException = e.getCause() != null ? e.getCause() : e;
        
        log.error("Error executing task {} for topic {}: {}", taskId, topicName, actualException.getMessage(), actualException);
        
        var throwsMapping = workerMethod.getThrowsExceptionMappings().get(actualException.getClass());
        if (throwsMapping != null) {
            var errorCode = throwsMapping.getErrorCode();
            var errorMessage = StringUtils.hasText(throwsMapping.getErrorMessage()) 
                    ? throwsMapping.getErrorMessage() 
                    : actualException.getMessage();
            handleBpmnError(externalTask, externalTaskService, errorCode, errorMessage);
            return;
        }
        
        var errorAnnotation = AnnotatedElementUtils.findMergedAnnotation(actualException.getClass(), BPMError.class);
        if (errorAnnotation != null) {
            var errorCode = errorAnnotation.errorCode();
            var errorMessage = StringUtils.hasText(errorAnnotation.errorMessage()) 
                    ? errorAnnotation.errorMessage() 
                    : actualException.getMessage();
            handleBpmnError(externalTask, externalTaskService, errorCode, errorMessage);
            return;
        }
        
        var methodErrorAnnotation = AnnotatedElementUtils.findMergedAnnotation(workerMethod.getMethod(), BPMError.class);
        if (methodErrorAnnotation != null) {
            var errorMapping = findErrorMapping(methodErrorAnnotation, actualException);
            if (errorMapping.isPresent()) {
                var mapping = errorMapping.get();
                var errorMessage = StringUtils.hasText(mapping.errorMessage()) 
                        ? mapping.errorMessage() 
                        : actualException.getMessage();
                handleBpmnError(externalTask, externalTaskService, mapping.errorCode(), errorMessage);
                return;
            }
            
            if (StringUtils.hasText(methodErrorAnnotation.errorCode())) {
                var errorMessage = StringUtils.hasText(methodErrorAnnotation.errorMessage()) 
                        ? methodErrorAnnotation.errorMessage() 
                        : actualException.getMessage();
                handleBpmnError(externalTask, externalTaskService, methodErrorAnnotation.errorCode(), errorMessage);
                return;
            }
        }
        
        handleTechnicalFailure(externalTask, externalTaskService, actualException);
    }
    
    private Optional<BPMError.ErrorMapping> findErrorMapping(BPMError bmpErrorAnnotation, Throwable exception) {
        return Arrays.stream(bmpErrorAnnotation.errorMappings())
                .filter(mapping -> mapping.exception().isAssignableFrom(exception.getClass()))
                .findFirst();
    }
    
    private void handleBpmnError(ExternalTask externalTask, ExternalTaskService externalTaskService, String errorCode, String errorMessage) {
        log.info("Handling BPMN error for task {} with error code: {} and message: {}", 
                externalTask.getId(), errorCode, errorMessage);
        
        var variables = Map.<String, Object>of();
        externalTaskService.handleBpmnError(externalTask, errorCode, errorMessage, variables);
    }
    
    private void handleTechnicalFailure(ExternalTask externalTask, ExternalTaskService externalTaskService, Throwable exception) {
        var errorMessage = exception.getMessage() != null ? exception.getMessage() : exception.getClass().getSimpleName();
        var errorDetails = String.format("%s: %s", exception.getClass().getSimpleName(), errorMessage);
        
        log.warn("Handling technical failure for task {} with error: {}", externalTask.getId(), errorDetails);
        
        var retries = 3;
        var retryTimeout = 10000; // 10 seconds
        externalTaskService.handleFailure(externalTask, errorMessage, errorDetails, retries, retryTimeout);
    }
    
    private Object[] prepareMethodArguments(ExternalTask externalTask) {
        return workerMethod.getParameters().stream()
                .map(paramInfo -> resolveParameterValue(externalTask, paramInfo))
                .toArray();
    }
    
    private Object resolveParameterValue(ExternalTask externalTask, WorkerMethod.ParameterInfo paramInfo) {
        var variableName = paramInfo.getVariableName();
        
        if (paramInfo.getType().equals(ExternalTask.class)) {
            return externalTask;
        }
        
        var value = externalTask.getAllVariables().get(variableName);
        
        if (value == null) {
            if (paramInfo.isRequired()) {
                throw new IllegalArgumentException("Required variable '" + variableName + "' not found");
            }
            
            if (StringUtils.hasText(paramInfo.getDefaultValue())) {
                value = paramInfo.getDefaultValue();
            }
        }
        
        return convertValue(value, paramInfo.getType());
    }
    
    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        
        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }
        
        try {
            return objectMapper.convertValue(value, targetType);
        } catch (Exception e) {
            log.warn("Could not convert value {} to type {}: {}", value, targetType, e.getMessage());
            return value;
        }
    }
    
    private Map<String, Object> processResult(Object result) {
        if (workerMethod.getResultAnnotation() == null) {
            return Map.of();
        }
        
        var resultAnnotation = workerMethod.getResultAnnotation();
        
        if (result == null) {
            return handleNullResult(resultAnnotation);
        }
        
        if (resultAnnotation.flatten()) {
            return flattenResult(result, resultAnnotation);
        }
        
        return Map.of(resultAnnotation.name(), result);
    }
    
    private Map<String, Object> handleNullResult(BPMResult resultAnnotation) {
        return switch (resultAnnotation.nullHandling()) {
            case SET_NULL -> {
                Map<String, Object> result = new HashMap<>();
                result.put(resultAnnotation.name(), null);
                yield result;
            }
            case SKIP -> Map.of();
            case THROW_EXCEPTION -> throw new IllegalStateException("Method returned null but null handling is set to THROW_EXCEPTION");
        };
    }
    
    private Map<String, Object> flattenResult(Object result, BPMResult resultAnnotation) {
        try {
            @SuppressWarnings("unchecked")
            var resultMap = (Map<String, Object>) objectMapper.convertValue(result, Map.class);
            
            return resultMap.entrySet().stream()
                    .filter(entry -> resultAnnotation.includeNullProperties() || entry.getValue() != null)
                    .collect(
                            HashMap::new,
                            (map, entry) -> {
                                var key = StringUtils.hasText(resultAnnotation.flattenPrefix()) 
                                        ? resultAnnotation.flattenPrefix() + entry.getKey()
                                        : entry.getKey();
                                map.put(key, entry.getValue());
                            },
                            HashMap::putAll
                    );
            
        } catch (Exception e) {
            log.warn("Could not flatten result object: {}", e.getMessage());
            return Map.of(resultAnnotation.name(), result);
        }
    }
}
