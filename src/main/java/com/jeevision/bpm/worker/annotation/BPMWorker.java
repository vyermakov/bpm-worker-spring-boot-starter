package com.jeevision.bpm.worker.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
public @interface BPMWorker {
    
    /**
     * The topic name for the external task worker.
     * This is a shortcut to the 'topic' parameter.
     */
    String value() default "";
    
    /**
     * The topic name for the external task worker.
     */
    String topic() default "";
    
    /**
     * Number of tasks to fetch and lock at once.
     */
    int fetchSize() default 1;
    
    /**
     * Timeout in milliseconds for how long a task is locked.
     */
    long lockDuration() default 30000;
    
    /**
     * Async response timeout in milliseconds.
     */
    long asyncResponseTimeout() default 10000;
}