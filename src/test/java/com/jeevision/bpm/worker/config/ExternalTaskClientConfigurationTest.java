package com.jeevision.bpm.worker.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jeevision.bpm.worker.registry.BPMWorkerRegistry;
import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.bpm.client.ExternalTaskClientBuilder;
import org.camunda.bpm.client.interceptor.ClientRequestContext;
import org.camunda.bpm.client.interceptor.ClientRequestInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.event.ContextRefreshedEvent;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExternalTaskClientConfigurationTest {

    @Mock
    private BPMWorkerProperties properties;
    
    @Mock
    private BPMWorkerProperties.Authentication auth;
    
    @Mock
    private BPMWorkerRegistry workerRegistry;
    
    @Mock
    private ObjectMapper objectMapper;
    
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
        Set<String> topics = Set.of("topic1", "topic2");
        when(workerRegistry.getRegisteredTopics()).thenReturn(topics);
        
        // Use reflection to set the private externalTaskClient field
        Field field = ExternalTaskClientConfiguration.class.getDeclaredField("externalTaskClient");
        field.setAccessible(true);
        field.set(configuration, externalTaskClient);
        
        // When
        configuration.subscribeToTopics();
        
        // Then
        verify(externalTaskClient, times(2)).subscribe(anyString());
        verify(workerRegistry).getRegisteredTopics();
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
        when(workerRegistry.getRegisteredTopics()).thenReturn(Set.of());
        
        // Use reflection to set the private externalTaskClient field
        Field field = ExternalTaskClientConfiguration.class.getDeclaredField("externalTaskClient");
        field.setAccessible(true);
        field.set(configuration, externalTaskClient);
        
        // When
        configuration.subscribeToTopics();
        
        // Then
        verify(externalTaskClient, never()).subscribe(anyString());
        verify(workerRegistry).getRegisteredTopics();
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
