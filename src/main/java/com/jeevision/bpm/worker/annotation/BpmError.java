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
public @interface BpmError {
    
    String code();
    String message() default "";
}