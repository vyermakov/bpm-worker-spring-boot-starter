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
    
    String name() default "result";
    boolean flatten() default false;
    String flattenPrefix() default "";
    NullHandling nullHandling() default NullHandling.SET_NULL;
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