package com.jeevision.bpm.worker.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to inject Camunda process or task variables into method parameters.
 * If no value is specified, the parameter name will be used as the variable name.
 *
 * @author Slava Yermakov
 * @email v.yermakov@gmail.com
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface BPMVariable {
    
    /**
     * The name of the process/task variable.
     * If not specified, the parameter name will be used.
     * Can be a SpEL expression (e.g. "#{'prefix-' + environment.getProperty('bpm.vars.suffix')}")
     */
    String value() default "";
    
    /**
     * Whether this variable is required.
     * If true and variable is not found, an exception will be thrown.
     */
    boolean required() default false;
    
    /**
     * Default value to use if variable is not found.
     */
    String defaultValue() default "";
}
