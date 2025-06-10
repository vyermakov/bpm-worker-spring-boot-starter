package com.jeevision.bpm.worker.config;

import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.bpm.client.ExternalTaskClientBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jeevision.bpm.worker.handler.BpmTaskHandler;
import com.jeevision.bpm.worker.registry.BpmWorkerRegistry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Configuration for Camunda External Task Client.
 *
 * @author Slava Yermakov
 * @email v.yermakov@gmail.com
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(BpmWorkerProperties.class)
public class ExternalTaskClientConfiguration {
    
    private final BpmWorkerProperties properties;
    private final BpmWorkerRegistry workerRegistry;
    private final ObjectMapper objectMapper;
    private final BpmTaskHandler bpmTaskHandler;
    
    @Bean
    public ExternalTaskClient externalTaskClient() {
        ExternalTaskClientBuilder builder = ExternalTaskClient.create()
                .baseUrl(properties.getBaseUrl())
                .workerId(properties.getWorkerId())
                .maxTasks(properties.getMaxTasks())
                .asyncResponseTimeout(properties.getAsyncResponseTimeout())
                .lockDuration(properties.getLockDuration())
                .usePriority(properties.isUsePriority());
        
        configureAuthentication(builder);
        
        return builder.build();
    }
    
	private void configureAuthentication(ExternalTaskClientBuilder builder) {
		BpmWorkerProperties.Authentication auth = properties.getAuth();

		if (StringUtils.hasText(auth.getUsername()) && StringUtils.hasText(auth.getPassword())) {
			builder.addInterceptor(new BasicAuthInterceptor(auth.getUsername(), auth.getPassword()));
			log.info("Configured basic authentication for Camunda client");
		} else if (StringUtils.hasText(auth.getToken())) {
			builder.addInterceptor(new BearerTokenInterceptor(auth.getToken()));
			log.info("Configured token-based authentication for Camunda client");
		}
	}
    
    @EventListener(ContextRefreshedEvent.class)
    public void subscribeToTopics() {
        ExternalTaskClient client = externalTaskClient();
        
        workerRegistry.getAllWorkerMethods().forEach((topic, workerMethod) -> {
            log.info("Subscribing to topic: {}", topic);
            
            client.subscribe(topic)
                    .lockDuration(workerMethod.getWorkerAnnotation().lockDuration())
                    .handler(new BpmTaskHandler(objectMapper, properties).withWorkerMethod(workerMethod))
                    .open();
        });
        
        log.info("Subscribed to {} BPM worker topics", workerRegistry.getRegisteredTopics().size());
    }
    
    private static class BearerTokenInterceptor implements org.camunda.bpm.client.interceptor.ClientRequestInterceptor {
        private final String token;
        
        public BearerTokenInterceptor(String token) {
            this.token = token;
        }
        
        @Override
        public void intercept(org.camunda.bpm.client.interceptor.ClientRequestContext requestContext) {
            requestContext.addHeader("Authorization", "Bearer " + token);
        }
    }
    
    private static class BasicAuthInterceptor implements org.camunda.bpm.client.interceptor.ClientRequestInterceptor {
    	
        private final String username;
        private final String password;

        public BasicAuthInterceptor(String username, String password) {
            this.username = username;
            this.password = password;
        }

        @Override
        public void intercept(org.camunda.bpm.client.interceptor.ClientRequestContext requestContext) {
            String credentials = username + ":" + password;
            String encodedCredentials = java.util.Base64.getEncoder().encodeToString(credentials.getBytes());
            requestContext.addHeader("Authorization", "Basic " + encodedCredentials);
        }
    }
}
