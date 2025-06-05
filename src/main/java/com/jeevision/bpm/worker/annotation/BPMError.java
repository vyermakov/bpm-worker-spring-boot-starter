package com.jeevision.bpm.worker.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to map exceptions to BPMN errors.
 * Can be applied to method parameters in throws clause or exception classes.
 * When an annotated exception is thrown, it will be handled as a BPMN error
 * instead of a technical failure/incident.
 *
 * @author Slava Yermakov
 * @email v.yermakov@gmail.com
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface BPMError {
    
    /**
     * The BPMN error code to be used when this exception occurs.
     */
    String errorCode();
    
    /**
     * Optional error message. If not provided, the exception message will be used.
     */
    String errorMessage() default "";
    
    /**
     * Map specific exception types to error codes.
     * Alternative to using the annotation on exception classes directly.
     */
    ErrorMapping[] errorMappings() default {};
    
    /**
     * Mapping between exception types and BPMN error codes.
     */
    @interface ErrorMapping {
        /**
         * The exception class to map.
         */
        Class<? extends Throwable> exception();
        
        /**
         * The BPMN error code for this exception.
         */
        String errorCode();
        
        /**
         * Optional error message for this exception type.
         */
        String errorMessage() default "";
    }
    
}