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
    
    private String baseUrl = "http://localhost:8080/engine-rest";
    private String workerId = "spring-boot-worker";
    private int maxTasks = 10;
    private long asyncResponseTimeout = 10000;
    private long lockDuration = 30000;
    private boolean usePriority = true;
    private Authentication auth = new Authentication();
    
    @Data
    public static class Authentication {
        private String username;
        private String password;
        private String token;
    }
}