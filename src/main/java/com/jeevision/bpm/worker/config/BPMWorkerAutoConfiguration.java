package com.jeevision.bpm.worker.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jeevision.bpm.worker.handler.BPMTaskHandler;
import com.jeevision.bpm.worker.registry.BPMWorkerRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for BPM Worker Spring Boot Starter.
 *
 * @author Slava Yermakov
 * @email v.yermakov@gmail.com
 */
@AutoConfiguration(after = JacksonAutoConfiguration.class)
@ConditionalOnClass(name = "org.camunda.bpm.client.ExternalTaskClient")
@ConditionalOnProperty(prefix = "bpm.worker", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(BPMWorkerProperties.class)
@Import({ExternalTaskClientConfiguration.class})
public class BPMWorkerAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public BPMWorkerRegistry bpmWorkerRegistry(ApplicationContext applicationContext) {
        return new BPMWorkerRegistry(applicationContext);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public BPMTaskHandler bmpTaskHandler(ObjectMapper objectMapper) {
        return new BPMTaskHandler(objectMapper);
    }
}