package com.jeevision.bpm.worker.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to automatically set the method return value as a process variable.
 * Supports object flattening and null handling strategies.
 *
 * @author Slava Yermakov
 * @email v.yermakov@gmail.com
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BPMResult {
    
    /**
     * The name of the process variable to set.
     * If not specified, "result" will be used.
     */
    String name() default "result";
    
    /**
     * Whether to flatten the object properties as separate variables.
     * If true, object properties will be set as individual process variables.
     */
    boolean flatten() default false;
    
    /**
     * Prefix to use when flattening object properties.
     * Only applicable when flatten is true.
     */
    String flattenPrefix() default "";
    
    /**
     * Strategy for handling null return values.
     */
    NullHandling nullHandling() default NullHandling.SET_NULL;
    
    /**
     * Whether to include null properties when flattening objects.
     * Only applicable when flatten is true.
     */
    boolean includeNullProperties() default false;
    
    enum NullHandling {
        /** Set the variable to null */
        SET_NULL,
        /** Skip setting the variable if return value is null */
        SKIP,
        /** Throw an exception if return value is null */
        THROW_EXCEPTION
    }
}