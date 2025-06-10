package com.jeevision.bpm.worker.registry;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.jeevision.bpm.worker.annotation.BpmError;
import com.jeevision.bpm.worker.annotation.BpmResult;
import com.jeevision.bpm.worker.annotation.BpmVariable;
import com.jeevision.bpm.worker.annotation.BpmWorker;
import com.jeevision.bpm.worker.model.WorkerMethod;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Registry for BPM worker methods.
 * Scans Spring beans for @BPMWorker annotated methods and registers them.
 *
 * @author Slava Yermakov
 * @email v.yermakov@gmail.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BpmWorkerRegistry implements BeanPostProcessor {
    
    private final Map<String, WorkerMethod> workerMethods = new ConcurrentHashMap<>();
    private final ApplicationContext applicationContext;
    private final ExpressionParser expressionParser;
    
    public BpmWorkerRegistry(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.expressionParser = createExpressionParser();
    }
    
    protected ExpressionParser createExpressionParser() {
        return new SpelExpressionParser();
    }
    
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> beanClass = bean.getClass();
        
        Stream.of(beanClass.getDeclaredMethods())
                .filter(method -> AnnotatedElementUtils.hasAnnotation(method, BpmWorker.class))
                .forEach(method -> registerWorkerMethod(bean, method));
        
        return bean;
    }
    
    private void registerWorkerMethod(Object bean, Method method) {
        BpmWorker workerAnnotation = AnnotatedElementUtils.findMergedAnnotation(method, BpmWorker.class);
        if (workerAnnotation == null) {
            return;
        }
        
        String topic = determineTopic(workerAnnotation);
        if (!StringUtils.hasText(topic)) {
            log.warn("No topic specified for worker method {}.{}", bean.getClass().getSimpleName(), method.getName());
            return;
        }
        
        List<WorkerMethod.ParameterInfo> parameters = extractParameters(method);
        BpmResult resultAnnotation = AnnotatedElementUtils.findMergedAnnotation(method, BpmResult.class);
        Map<Class<? extends Throwable>, WorkerMethod.ThrowsExceptionInfo> exceptionMappings = extractExceptionMappings(method);
        
        WorkerMethod workerMethod = WorkerMethod.builder()
                .bean(bean)
                .method(method)
                .workerAnnotation(workerAnnotation)
                .resultAnnotation(resultAnnotation)
                .parameters(parameters)
                .topic(topic)
                .throwsExceptionMappings(exceptionMappings)
                .build();
        
        workerMethods.put(topic, workerMethod);
        log.info("Registered BPM worker for topic '{}' -> {}.{}", topic, bean.getClass().getSimpleName(), method.getName());
    }
    
    private String determineTopic(BpmWorker annotation) {
        String topic = StringUtils.hasText(annotation.value()) ? annotation.value() : annotation.topic();
        return evaluateSpelExpression(topic);
    }
    
    private List<WorkerMethod.ParameterInfo> extractParameters(Method method) {
        return Stream.of(method.getParameters())
                .map(this::createParameterInfo)
                .toList();
    }
    
    private WorkerMethod.ParameterInfo createParameterInfo(Parameter parameter) {
        BpmVariable variableAnnotation = parameter.getAnnotation(BpmVariable.class);
        String variableName = determineVariableName(parameter, variableAnnotation);
        
        return WorkerMethod.ParameterInfo.builder()
                .parameter(parameter)
                .variableAnnotation(variableAnnotation)
                .variableName(variableName)
                .required(variableAnnotation != null && variableAnnotation.required())
                .defaultValue(variableAnnotation != null ? variableAnnotation.defaultValue() : "")
                .type(parameter.getType())
                .build();
    }
    
    private String determineVariableName(Parameter parameter, BpmVariable annotation) {
        if (annotation != null && StringUtils.hasText(annotation.value())) {
            return evaluateSpelExpression(annotation.value());
        }
        return parameter.getName();
    }

    String evaluateSpelExpression(String expression) {
        if (!StringUtils.hasText(expression)) {
            return expression;
        }

        if (expression.matches(".*#\\{.+\\}.*")) {
            try {
                StandardEvaluationContext context = new StandardEvaluationContext();
                context.setBeanResolver(new BeanFactoryResolver(applicationContext));
                
                String spelContent = expression.replaceAll(".*#\\{(.+)\\}.*", "$1");
                Expression exp = expressionParser.parseExpression(spelContent);
                Object result = exp.getValue(context);
                return result != null ? result.toString() : expression;
            } catch (Exception e) {
                log.error("Failed to evaluate SpEL expression '{}'", expression, e);
                throw new IllegalArgumentException("Invalid SpEL expression: " + expression, e);
            }
        }
        return expression;
    }
    
    public Optional<WorkerMethod> getWorkerMethod(String topic) {
        return Optional.ofNullable(workerMethods.get(topic));
    }
    
    public Set<String> getRegisteredTopics() {
        return new HashSet<>(workerMethods.keySet());
    }
    
    public Map<String, WorkerMethod> getAllWorkerMethods() {
        return Map.copyOf(workerMethods);
    }
    
    private Map<Class<? extends Throwable>, WorkerMethod.ThrowsExceptionInfo> extractExceptionMappings(Method method) {
        Map<Class<? extends Throwable>, WorkerMethod.ThrowsExceptionInfo> mappings = new HashMap<>();
        
        AnnotatedType[] annotatedExceptionTypes = method.getAnnotatedExceptionTypes();
        Class<?>[] exceptionTypes = method.getExceptionTypes();
        
        for (int i = 0; i < exceptionTypes.length; i++) {
            Class<?> exceptionType = exceptionTypes[i];
            AnnotatedType annotatedType = annotatedExceptionTypes[i];
            
            BpmError bpmError = annotatedType.getAnnotation(BpmError.class);
            if (bpmError != null) {
                @SuppressWarnings("unchecked")
                Class<? extends Throwable> throwableType = (Class<? extends Throwable>) exceptionType;
                
                WorkerMethod.ThrowsExceptionInfo exceptionInfo = WorkerMethod.ThrowsExceptionInfo.builder()
                        .exceptionType(throwableType)
                        .bpmErrorAnnotation(bpmError)
                        .errorCode(bpmError.errorCode())
                        .errorMessage(StringUtils.hasText(bpmError.errorMessage()) ? bpmError.errorMessage() : "")
                        .build();
                
                mappings.put(throwableType, exceptionInfo);
                
                log.debug("Registered BpmError mapping for exception {} with error code '{}'", 
                        exceptionType.getSimpleName(), bpmError.errorCode());
            }
        }
        
        return mappings;
    }
}
