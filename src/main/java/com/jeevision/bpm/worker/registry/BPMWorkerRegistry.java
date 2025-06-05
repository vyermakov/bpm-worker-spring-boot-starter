package com.jeevision.bpm.worker.registry;

import com.jeevision.bpm.worker.annotation.BPMResult;
import com.jeevision.bpm.worker.annotation.BPMVariable;
import com.jeevision.bpm.worker.annotation.BPMWorker;
import com.jeevision.bpm.worker.model.WorkerMethod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Stream;

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
public class BPMWorkerRegistry implements BeanPostProcessor {
    
    private final Map<String, WorkerMethod> workerMethods = new HashMap<>();
    private final ApplicationContext applicationContext;
    
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> beanClass = bean.getClass();
        
        Stream.of(beanClass.getDeclaredMethods())
                .filter(method -> AnnotatedElementUtils.hasAnnotation(method, BPMWorker.class))
                .forEach(method -> registerWorkerMethod(bean, method));
        
        return bean;
    }
    
    private void registerWorkerMethod(Object bean, Method method) {
        BPMWorker workerAnnotation = AnnotatedElementUtils.findMergedAnnotation(method, BPMWorker.class);
        if (workerAnnotation == null) {
            return;
        }
        
        String topic = determineTopic(workerAnnotation);
        if (!StringUtils.hasText(topic)) {
            log.warn("No topic specified for worker method {}.{}", bean.getClass().getSimpleName(), method.getName());
            return;
        }
        
        List<WorkerMethod.ParameterInfo> parameters = extractParameters(method);
        BPMResult resultAnnotation = AnnotatedElementUtils.findMergedAnnotation(method, BPMResult.class);
        
        WorkerMethod workerMethod = WorkerMethod.builder()
                .bean(bean)
                .method(method)
                .workerAnnotation(workerAnnotation)
                .resultAnnotation(resultAnnotation)
                .parameters(parameters)
                .topic(topic)
                .build();
        
        workerMethods.put(topic, workerMethod);
        log.info("Registered BPM worker for topic '{}' -> {}.{}", topic, bean.getClass().getSimpleName(), method.getName());
    }
    
    private String determineTopic(BPMWorker annotation) {
        String topic = StringUtils.hasText(annotation.value()) ? annotation.value() : annotation.topic();
        return evaluateSpelExpression(topic);
    }
    
    private List<WorkerMethod.ParameterInfo> extractParameters(Method method) {
        return Stream.of(method.getParameters())
                .map(this::createParameterInfo)
                .toList();
    }
    
    private WorkerMethod.ParameterInfo createParameterInfo(Parameter parameter) {
        BPMVariable variableAnnotation = parameter.getAnnotation(BPMVariable.class);
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
    
    private String determineVariableName(Parameter parameter, BPMVariable annotation) {
        if (annotation != null && StringUtils.hasText(annotation.value())) {
            return evaluateSpelExpression(annotation.value());
        }
        return parameter.getName();
    }

    private String evaluateSpelExpression(String expression) {
        if (!StringUtils.hasText(expression)) {
            return expression;
        }

        // Simple regex to match #{...} expressions
        if (expression.matches(".*#\\{.+\\}.*")) {
            try {
                ExpressionParser parser = new SpelExpressionParser();
                StandardEvaluationContext context = new StandardEvaluationContext();
                context.setBeanResolver(new BeanFactoryResolver(applicationContext));
                
                // Extract just the expression content between #{ and }
                String spelContent = expression.replaceAll(".*#\\{(.+)\\}.*", "$1");
                Expression exp = parser.parseExpression(spelContent);
                Object result = exp.getValue(context);
                return result != null ? result.toString() : expression;
            } catch (Exception e) {
                log.warn("Failed to evaluate SpEL expression '{}': {}", expression, e.getMessage());
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
}
