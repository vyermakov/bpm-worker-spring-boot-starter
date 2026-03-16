package com.jeevision.bpm.worker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

import java.net.InetAddress;

/**
 * Configuration properties for BPM Worker.
 *
 * @author Slava Yermakov
 * @email v.yermakov@gmail.com
 */
@Data
@ConfigurationProperties(prefix = "bpm.worker")
public class BpmWorkerProperties {
    
    private String baseUrl = "http://localhost:8080/engine-rest";
    private String workerId = getDefaultWorkerId();
    private int maxTasks = 10;
    private long asyncResponseTimeout = 10000;
    private long lockDuration = 30000;
    private boolean usePriority = true;
    private Authentication auth = new Authentication();
    private Retry retry = new Retry();
    
    private static String getDefaultWorkerId() {
        try {
            String hostName = InetAddress.getLocalHost().getHostName();
            return "spring-boot-worker-" + hostName;
        } catch (Exception e) {
            return "spring-boot-worker";
        }
    }
    
    @Data
    public static class Authentication {
        private String username;
        private String password;
        private String token;
    }
    
    @Data
    public static class Retry {
        private int maxRetries = 3;
        private long retryTimeout = 60000; // 1 minute in milliseconds
        private boolean useExponentialBackoff = false;
        private double backoffMultiplier = 2.0;
    }
}