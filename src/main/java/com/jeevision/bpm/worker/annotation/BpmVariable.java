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
public @interface BpmVariable {
    
    String value() default "";
    boolean required() default false;
    String defaultValue() default "";
}
