package com.jeevision.bpm.worker.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Set;

import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.bpm.client.ExternalTaskClientBuilder;
import org.camunda.bpm.client.interceptor.ClientRequestContext;
import org.camunda.bpm.client.interceptor.ClientRequestInterceptor;
import org.camunda.bpm.client.topic.TopicSubscriptionBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.event.ContextRefreshedEvent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jeevision.bpm.worker.handler.BpmTaskHandler;
import com.jeevision.bpm.worker.registry.BpmWorkerRegistry;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExternalTaskClientConfigurationTest {

    @Mock
    private BpmWorkerProperties properties;
    
    @Mock
    private BpmWorkerProperties.Authentication auth;
    
    @Mock
    private BpmWorkerRegistry workerRegistry;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @Mock
    private BpmTaskHandler bpmTaskHandler;
    
    @Mock
    private ExternalTaskClientBuilder clientBuilder;
    
    @Mock
    private ExternalTaskClient externalTaskClient;
    
    @Mock
    private ContextRefreshedEvent contextRefreshedEvent;
    
    private ExternalTaskClientConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new ExternalTaskClientConfiguration(properties, workerRegistry, objectMapper);
        lenient().when(properties.getAuth()).thenReturn(auth);
        lenient().when(properties.getWorkerId()).thenReturn(null);
        lenient().when(properties.getMaxTasks()).thenReturn(10);
        lenient().when(properties.getAsyncResponseTimeout()).thenReturn(5000L);
        lenient().when(properties.getLockDuration()).thenReturn(10000L);
        lenient().when(properties.isUsePriority()).thenReturn(false);
    }

    @Test
    void testExternalTaskClient_WithBasicAuth() {
        // Given
        when(properties.getBaseUrl()).thenReturn("http://localhost:8080/engine-rest");
        when(auth.getUsername()).thenReturn("testuser");
        when(auth.getPassword()).thenReturn("testpass");
        when(auth.getToken()).thenReturn("");
        
        try (MockedStatic<ExternalTaskClient> mockedStatic = mockStatic(ExternalTaskClient.class)) {
            mockedStatic.when(() -> ExternalTaskClient.create()).thenReturn(clientBuilder);
            when(clientBuilder.baseUrl(anyString())).thenReturn(clientBuilder);
            when(clientBuilder.workerId(isNull())).thenReturn(clientBuilder);
            when(clientBuilder.maxTasks(anyInt())).thenReturn(clientBuilder);
            when(clientBuilder.asyncResponseTimeout(anyLong())).thenReturn(clientBuilder);
            when(clientBuilder.lockDuration(anyLong())).thenReturn(clientBuilder);
            when(clientBuilder.usePriority(anyBoolean())).thenReturn(clientBuilder);
            when(clientBuilder.addInterceptor(any(ClientRequestInterceptor.class))).thenReturn(clientBuilder);
            when(clientBuilder.build()).thenReturn(externalTaskClient);
            
            // When
            ExternalTaskClient result = configuration.externalTaskClient();
            
            // Then
            assertNotNull(result);
            verify(clientBuilder).baseUrl("http://localhost:8080/engine-rest");
            verify(clientBuilder).addInterceptor(any(ClientRequestInterceptor.class));
            verify(clientBuilder).build();
        }
    }

    @Test
    void testExternalTaskClient_WithBearerToken() {
        // Given
        when(properties.getBaseUrl()).thenReturn("http://localhost:8080/engine-rest");
        when(auth.getUsername()).thenReturn("");
        when(auth.getPassword()).thenReturn("");
        when(auth.getToken()).thenReturn("test-token");
        
        try (MockedStatic<ExternalTaskClient> mockedStatic = mockStatic(ExternalTaskClient.class)) {
            mockedStatic.when(() -> ExternalTaskClient.create()).thenReturn(clientBuilder);
            when(clientBuilder.baseUrl(anyString())).thenReturn(clientBuilder);
            when(clientBuilder.workerId(isNull())).thenReturn(clientBuilder);
            when(clientBuilder.maxTasks(anyInt())).thenReturn(clientBuilder);
            when(clientBuilder.asyncResponseTimeout(anyLong())).thenReturn(clientBuilder);
            when(clientBuilder.lockDuration(anyLong())).thenReturn(clientBuilder);
            when(clientBuilder.usePriority(anyBoolean())).thenReturn(clientBuilder);
            when(clientBuilder.addInterceptor(any(ClientRequestInterceptor.class))).thenReturn(clientBuilder);
            when(clientBuilder.build()).thenReturn(externalTaskClient);
            
            // When
            ExternalTaskClient result = configuration.externalTaskClient();
            
            // Then
            assertNotNull(result);
            verify(clientBuilder).baseUrl("http://localhost:8080/engine-rest");
            verify(clientBuilder).addInterceptor(any(ClientRequestInterceptor.class));
            verify(clientBuilder).build();
        }
    }

    @Test
    void testExternalTaskClient_WithoutAuthentication() {
        // Given
        when(properties.getBaseUrl()).thenReturn("http://localhost:8080/engine-rest");
        when(auth.getUsername()).thenReturn("");
        when(auth.getPassword()).thenReturn("");
        when(auth.getToken()).thenReturn("");
        
        try (MockedStatic<ExternalTaskClient> mockedStatic = mockStatic(ExternalTaskClient.class)) {
            mockedStatic.when(() -> ExternalTaskClient.create()).thenReturn(clientBuilder);
            when(clientBuilder.baseUrl(anyString())).thenReturn(clientBuilder);
            when(clientBuilder.workerId(isNull())).thenReturn(clientBuilder);
            when(clientBuilder.maxTasks(anyInt())).thenReturn(clientBuilder);
            when(clientBuilder.asyncResponseTimeout(anyLong())).thenReturn(clientBuilder);
            when(clientBuilder.lockDuration(anyLong())).thenReturn(clientBuilder);
            when(clientBuilder.usePriority(anyBoolean())).thenReturn(clientBuilder);
            when(clientBuilder.build()).thenReturn(externalTaskClient);
            
            // When
            ExternalTaskClient result = configuration.externalTaskClient();
            
            // Then
            assertNotNull(result);
            verify(clientBuilder).baseUrl("http://localhost:8080/engine-rest");
            verify(clientBuilder, never()).addInterceptor(any(ClientRequestInterceptor.class));
            verify(clientBuilder).build();
        }
    }

    @Test
    void testSubscribeToTopics() throws Exception {
        // Given
        when(workerRegistry.getAllWorkerMethods()).thenReturn(Map.of(
            "topic1", mock(com.jeevision.bpm.worker.model.WorkerMethod.class),
            "topic2", mock(com.jeevision.bpm.worker.model.WorkerMethod.class)
        ));
        when(workerRegistry.getRegisteredTopics()).thenReturn(Set.of("topic1", "topic2"));
        
        var mockWorkerAnnotation = mock(com.jeevision.bpm.worker.annotation.BpmWorker.class);
        when(mockWorkerAnnotation.lockDuration()).thenReturn(10000L);
        
        for (var workerMethod : workerRegistry.getAllWorkerMethods().values()) {
            when(workerMethod.getWorkerAnnotation()).thenReturn(mockWorkerAnnotation);
        }
        
        // Mock the subscription builder chain - using generic Object since exact classes may not be available
        var mockTopicSubscriptionBuilder = mock(TopicSubscriptionBuilder.class);
        when(mockTopicSubscriptionBuilder.lockDuration(anyLong())).thenReturn(mockTopicSubscriptionBuilder);
        when(mockTopicSubscriptionBuilder.handler(any())).thenReturn(mockTopicSubscriptionBuilder);
        when(externalTaskClient.subscribe(anyString())).thenReturn(mockTopicSubscriptionBuilder);
        
        // Set up the external task client to be available for subscription
        when(properties.getBaseUrl()).thenReturn("http://localhost:8080/engine-rest");
        when(auth.getUsername()).thenReturn("");
        when(auth.getPassword()).thenReturn("");
        when(auth.getToken()).thenReturn("");
        
        try (MockedStatic<ExternalTaskClient> mockedStatic = mockStatic(ExternalTaskClient.class)) {
            mockedStatic.when(() -> ExternalTaskClient.create()).thenReturn(clientBuilder);
            when(clientBuilder.baseUrl(anyString())).thenReturn(clientBuilder);
            when(clientBuilder.workerId(isNull())).thenReturn(clientBuilder);
            when(clientBuilder.maxTasks(anyInt())).thenReturn(clientBuilder);
            when(clientBuilder.asyncResponseTimeout(anyLong())).thenReturn(clientBuilder);
            when(clientBuilder.lockDuration(anyLong())).thenReturn(clientBuilder);
            when(clientBuilder.usePriority(anyBoolean())).thenReturn(clientBuilder);
            when(clientBuilder.build()).thenReturn(externalTaskClient);
            
            // Create the client first to ensure it's available
            configuration.externalTaskClient();
            
            // When
            configuration.subscribeToTopics();
            
            // Then
            verify(externalTaskClient, times(2)).subscribe(anyString());
            verify(workerRegistry, times(2)).getAllWorkerMethods();
        }
    }

    @Test
    void testBearerTokenInterceptor() throws Exception {
        // Given
        String token = "test-bearer-token";
        
        // Use reflection to access the private inner class
        Class<?> interceptorClass = Class.forName("com.jeevision.bpm.worker.config.ExternalTaskClientConfiguration$BearerTokenInterceptor");
        Constructor<?> constructor = interceptorClass.getDeclaredConstructor(String.class);
        constructor.setAccessible(true);
        ClientRequestInterceptor interceptor = (ClientRequestInterceptor) constructor.newInstance(token);
        
        ClientRequestContext mockContext = mock(ClientRequestContext.class);
        
        // When
        interceptor.intercept(mockContext);
        
        // Then
        verify(mockContext).addHeader("Authorization", "Bearer " + token);
    }

    @Test
    void testBasicAuthInterceptor() throws Exception {
        // Given
        String username = "testuser";
        String password = "testpass";
        
        // Use reflection to access the private inner class
        Class<?> interceptorClass = Class.forName("com.jeevision.bpm.worker.config.ExternalTaskClientConfiguration$BasicAuthInterceptor");
        Constructor<?> constructor = interceptorClass.getDeclaredConstructor(String.class, String.class);
        constructor.setAccessible(true);
        ClientRequestInterceptor interceptor = (ClientRequestInterceptor) constructor.newInstance(username, password);
        
        ClientRequestContext mockContext = mock(ClientRequestContext.class);
        
        // When
        interceptor.intercept(mockContext);
        
        // Then
        ArgumentCaptor<String> headerCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockContext).addHeader(eq("Authorization"), headerCaptor.capture());
        
        String authHeader = headerCaptor.getValue();
        assertTrue(authHeader.startsWith("Basic "));
    }

    @Test
    void testSubscribeToTopics_WithEmptyTopics() throws Exception {
        // Given
        when(workerRegistry.getAllWorkerMethods()).thenReturn(Map.of());
        when(workerRegistry.getRegisteredTopics()).thenReturn(Set.of());
        
        // Set up the external task client to be available for subscription
        when(properties.getBaseUrl()).thenReturn("http://localhost:8080/engine-rest");
        when(auth.getUsername()).thenReturn("");
        when(auth.getPassword()).thenReturn("");
        when(auth.getToken()).thenReturn("");
        
        try (MockedStatic<ExternalTaskClient> mockedStatic = mockStatic(ExternalTaskClient.class)) {
            mockedStatic.when(() -> ExternalTaskClient.create()).thenReturn(clientBuilder);
            when(clientBuilder.baseUrl(anyString())).thenReturn(clientBuilder);
            when(clientBuilder.workerId(isNull())).thenReturn(clientBuilder);
            when(clientBuilder.maxTasks(anyInt())).thenReturn(clientBuilder);
            when(clientBuilder.asyncResponseTimeout(anyLong())).thenReturn(clientBuilder);
            when(clientBuilder.lockDuration(anyLong())).thenReturn(clientBuilder);
            when(clientBuilder.usePriority(anyBoolean())).thenReturn(clientBuilder);
            when(clientBuilder.build()).thenReturn(externalTaskClient);
            
            // Create the client first to ensure it's available
            configuration.externalTaskClient();
            
            // When
            configuration.subscribeToTopics();
            
            // Then
            verify(externalTaskClient, never()).subscribe(anyString());
            verify(workerRegistry).getAllWorkerMethods();
        }
    }

    @Test
    void testConfigureAuthentication_PrioritizesBasicAuthOverToken() {
        // Given
        when(auth.getUsername()).thenReturn("testuser");
        when(auth.getPassword()).thenReturn("testpass");
        when(auth.getToken()).thenReturn("test-token");
        when(properties.getBaseUrl()).thenReturn("http://localhost:8080/engine-rest");
        
        try (MockedStatic<ExternalTaskClient> mockedStatic = mockStatic(ExternalTaskClient.class)) {
            mockedStatic.when(() -> ExternalTaskClient.create()).thenReturn(clientBuilder);
            when(clientBuilder.baseUrl(anyString())).thenReturn(clientBuilder);
            when(clientBuilder.workerId(isNull())).thenReturn(clientBuilder);
            when(clientBuilder.maxTasks(anyInt())).thenReturn(clientBuilder);
            when(clientBuilder.asyncResponseTimeout(anyLong())).thenReturn(clientBuilder);
            when(clientBuilder.lockDuration(anyLong())).thenReturn(clientBuilder);
            when(clientBuilder.usePriority(anyBoolean())).thenReturn(clientBuilder);
            when(clientBuilder.addInterceptor(any(ClientRequestInterceptor.class))).thenReturn(clientBuilder);
            when(clientBuilder.build()).thenReturn(externalTaskClient);
            
            // When
            configuration.externalTaskClient();
            
            // Then
            // Should only add one interceptor (basic auth takes priority)
            verify(clientBuilder, times(1)).addInterceptor(any(ClientRequestInterceptor.class));
        }
    }
}
