package com.jeevision.bpm.worker.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

/**
 * Annotation to mark a method as a Camunda external task worker.
 * The annotated method will be registered as a listener for the specified topic.
 *
 * @author Slava Yermakov
 * @email v.yermakov@gmail.com
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface BpmWorker {
    
    @AliasFor("topic")
    String value() default "";
    
    @AliasFor("value")
    String topic() default "";
    
    int fetchSize() default 1;
    long lockDuration() default 30000;
    long asyncResponseTimeout() default 10000;
}
