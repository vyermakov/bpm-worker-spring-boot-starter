package com.jeevision.bpm.worker.model;

import com.jeevision.bpm.worker.annotation.BPMError;
import com.jeevision.bpm.worker.annotation.BPMResult;
import com.jeevision.bpm.worker.annotation.BPMVariable;
import com.jeevision.bpm.worker.annotation.BPMWorker;
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
    private BPMWorker workerAnnotation;
    private BPMResult resultAnnotation;
    private List<ParameterInfo> parameters;
    private String topic;
    private Map<Class<? extends Throwable>, ThrowsExceptionInfo> throwsExceptionMappings;
    
    @Data
    @Builder
    public static class ParameterInfo {
        private Parameter parameter;
        private BPMVariable variableAnnotation;
        private String variableName;
        private boolean required;
        private String defaultValue;
        private Class<?> type;
    }
    
    @Data
    @Builder
    public static class ThrowsExceptionInfo {
        private Class<? extends Throwable> exceptionType;
        private BPMError bmpErrorAnnotation;
        private String errorCode;
        private String errorMessage;
    }
}