package com.jeevision.bpm.worker.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for BPM Worker.
 *
 * @author Slava Yermakov
 * @email v.yermakov@gmail.com
 */
@Data
@ConfigurationProperties(prefix = "bpm.worker")
public class BPMWorkerProperties {
    
    /**
     * Base URL of the Camunda REST API.
     */
    private String baseUrl = "http://localhost:8080/engine-rest";
    
    /**
     * Worker ID used to identify this worker.
     */
    private String workerId = "spring-boot-worker";
    
    /**
     * Maximum number of tasks to fetch at once.
     */
    private int maxTasks = 10;
    
    /**
     * Async response timeout in milliseconds.
     */
    private long asyncResponseTimeout = 10000;
    
    /**
     * Lock duration in milliseconds.
     */
    private long lockDuration = 30000;
    
    /**
     * Whether to use priority when fetching tasks.
     */
    private boolean usePriority = true;
    
    /**
     * Authentication configuration.
     */
    private Authentication auth = new Authentication();
    
    @Data
    public static class Authentication {
        /**
         * Username for basic authentication.
         */
        private String username;
        
        /**
         * Password for basic authentication.
         */
        private String password;
        
        /**
         * Bearer token for token-based authentication.
         */
        private String token;
    }
}