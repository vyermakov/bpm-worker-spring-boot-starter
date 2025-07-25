package com.jeevision.bpm.worker.handler;

import java.util.HashMap;
import java.util.Map;

import org.cibseven.bpm.client.task.ExternalTask;
import org.cibseven.bpm.client.task.ExternalTaskHandler;
import org.cibseven.bpm.client.task.ExternalTaskService;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jeevision.bpm.worker.annotation.BpmResult;
import com.jeevision.bpm.worker.config.BpmWorkerProperties;
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
public class BpmTaskHandler implements ExternalTaskHandler {
    
    private final ObjectMapper objectMapper;
    private final BpmWorkerProperties properties;
    private WorkerMethod workerMethod;
    
    public BpmTaskHandler withWorkerMethod(WorkerMethod workerMethod) {
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
        
        var value = externalTask.getVariable(variableName);
        
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
        
        return Map.of(resultAnnotation.value(), result);
    }
    
    private Map<String, Object> handleNullResult(BpmResult resultAnnotation) {
        return switch (resultAnnotation.nullHandling()) {
            case SET_NULL -> {
                Map<String, Object> result = new HashMap<>();
                result.put(resultAnnotation.value(), null);
                yield result;
            }
            case SKIP -> Map.of();
            case THROW_EXCEPTION -> throw new IllegalStateException("Method returned null but null handling is set to THROW_EXCEPTION");
        };
    }
    
    private Map<String, Object> flattenResult(Object result, BpmResult resultAnnotation) {
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
            return Map.of(resultAnnotation.value(), result);
        }
    }
    
    private void handleException(ExternalTask externalTask, ExternalTaskService externalTaskService, Exception exception) {
        Throwable cause = exception.getCause() != null ? exception.getCause() : exception;
        
        // Check if this exception type is mapped to a BpmError
        var exceptionMapping = workerMethod.getThrowsExceptionMappings().get(cause.getClass());
        
        if (exceptionMapping != null) {
            // Report as BPMN error
            String errorCode = exceptionMapping.getErrorCode();
            String errorMessage = StringUtils.hasText(exceptionMapping.getErrorMessage()) 
                    ? exceptionMapping.getErrorMessage() 
                    : cause.getMessage();
            
            log.info("Handling BPMN error for task {} with code '{}': {}", 
                    externalTask.getId(), errorCode, errorMessage);
            
            externalTaskService.handleBpmnError(externalTask, errorCode, errorMessage);
        } else {
            // Report as technical failure/incident with retry configuration
            String errorMessage = cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
            
            int maxRetries = properties.getRetry().getMaxRetries();
            int currentRetries = externalTask.getRetries() != null ? externalTask.getRetries() : maxRetries;
            long retryTimeout = calculateRetryTimeout(currentRetries);
            
            log.error("Handling technical failure for task {} (retry {}/{}, backoff {}ms): {}", 
                    externalTask.getId(), maxRetries - currentRetries + 1, maxRetries, retryTimeout, errorMessage, cause);
            
            externalTaskService.handleFailure(externalTask, errorMessage, 
                    cause.toString(), --currentRetries, retryTimeout);
        }
    }
    
    private long calculateRetryTimeout(int currentRetries) {
        var retryConfig = properties.getRetry();
        long baseTimeout = retryConfig.getRetryTimeout();
        int maxRetries = retryConfig.getMaxRetries();
        
        if (!retryConfig.isUseExponentialBackoff()) {
            return baseTimeout;
        }
        
        // Calculate exponential backoff
        return (long) (baseTimeout * Math.pow(2, (maxRetries - currentRetries)));
    }
}
