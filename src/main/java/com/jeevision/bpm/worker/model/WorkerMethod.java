package com.jeevision.bpm.worker.model;

import com.jeevision.bpm.worker.annotation.BpmError;
import com.jeevision.bpm.worker.annotation.BpmResult;
import com.jeevision.bpm.worker.annotation.BpmVariable;
import com.jeevision.bpm.worker.annotation.BpmWorker;
import lombok.Builder;
import lombok.Data;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;

/**
 * Represents a worker method with its metadata.
 *
 * @author Slava Yermakov
 * @email v.yermakov@gmail.com
 */
@Data
@Builder
public class WorkerMethod {
    private Object bean;
    private Method method;
    private BpmWorker workerAnnotation;
    private BpmResult resultAnnotation;
    private List<ParameterInfo> parameters;
    private String topic;
    private Map<Class<? extends Throwable>, ThrowsExceptionInfo> throwsExceptionMappings;
    private List<ThrowsExceptionInfo> throwsExceptions;
    
    @Data
    @Builder
    public static class ParameterInfo {
        private Parameter parameter;
        private BpmVariable variableAnnotation;
        private String variableName;
        private boolean required;
        private String defaultValue;
        private Class<?> type;
    }
    
    @Data
    @Builder
    public static class ThrowsExceptionInfo {
        private Class<? extends Throwable> exceptionType;
        private BpmError bpmErrorAnnotation;
        private String errorCode;
        private String errorMessage;
    }
}
