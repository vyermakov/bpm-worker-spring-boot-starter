package com.jeevision.bpm.worker.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jeevision.bpm.worker.annotation.BPMError;
import com.jeevision.bpm.worker.annotation.BPMResult;
import com.jeevision.bpm.worker.annotation.BPMVariable;
import com.jeevision.bpm.worker.model.WorkerMethod;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BPMTaskHandlerTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ExternalTask externalTask;

    @Mock
    private ExternalTaskService externalTaskService;

    @InjectMocks
    private BPMTaskHandler taskHandler;

    private WorkerMethod workerMethod;
    private TestWorker testWorker;

    @BeforeEach
    void setUp() throws Exception {
        testWorker = new TestWorker();
        workerMethod = createWorkerMethod();
        taskHandler.withWorkerMethod(workerMethod);
    }

    @Test
    void testSuccessfulTaskExecution() throws Exception {
        // Setup
        when(externalTask.getAllVariables()).thenReturn(Map.of("param1", "value1"));
        when(externalTask.getId()).thenReturn("task123");
        when(externalTask.getTopicName()).thenReturn("testTopic");

        // Execute
        taskHandler.execute(externalTask, externalTaskService);

        // Verify
        verify(externalTaskService).complete(eq(externalTask), anyMap());
        assertEquals("processed-value1", testWorker.getLastResult());
    }

    @Test
    void testTaskExecutionWithResultProcessing() throws Exception {
        // Setup
        workerMethod = createWorkerMethodWithResultAnnotation();
        taskHandler.withWorkerMethod(workerMethod);
        
        when(externalTask.getAllVariables()).thenReturn(Map.of("param1", "value1"));
        when(externalTask.getId()).thenReturn("task123");
        when(externalTask.getTopicName()).thenReturn("testTopic");
        when(objectMapper.convertValue(any(), eq(Map.class))).thenReturn(Map.of("resultKey", "resultValue"));

        // Execute
        taskHandler.execute(externalTask, externalTaskService);

        // Verify
        verify(externalTaskService).complete(
            eq(externalTask), 
            argThat(vars -> vars.containsKey("resultKey") && "resultValue".equals(vars.get("resultKey")))
        );
    }

    @Test
    void testExceptionHandlingWithBPMErrorAnnotation() throws Exception {
        // Setup
        workerMethod = createWorkerMethodThatThrows();
        taskHandler.withWorkerMethod(workerMethod);
        
        when(externalTask.getAllVariables()).thenReturn(Map.of("param1", "value1"));
        when(externalTask.getId()).thenReturn("task123");
        when(externalTask.getTopicName()).thenReturn("testTopic");

        // Execute
        taskHandler.execute(externalTask, externalTaskService);

        // Verify
        verify(externalTaskService).handleBpmnError(
            eq(externalTask), 
            eq("TEST_ERROR"), 
            anyString(), 
            anyMap());
    }

    @Test
    void testTechnicalFailureHandling() throws Exception {
        // Setup
        workerMethod = createWorkerMethodThatThrowsRuntimeException();
        taskHandler.withWorkerMethod(workerMethod);
        
        when(externalTask.getAllVariables()).thenReturn(Map.of("param1", "value1"));
        when(externalTask.getId()).thenReturn("task123");
        when(externalTask.getTopicName()).thenReturn("testTopic");

        // Execute
        taskHandler.execute(externalTask, externalTaskService);

        // Verify
        verify(externalTaskService).handleFailure(
            eq(externalTask), 
            anyString(), 
            anyString(), 
            eq(3), 
            eq(10000L));
    }

    // Helper methods to create test worker methods
    private WorkerMethod createWorkerMethod() throws Exception {
        Method method = TestWorker.class.getMethod("processTask", String.class);
        return WorkerMethod.builder()
                .bean(testWorker)
                .method(method)
                .parameters(createParameterInfos(method))
                .throwsExceptionMappings(new HashMap<>())
                .build();
    }

    private List<WorkerMethod.ParameterInfo> createParameterInfos(Method method) throws Exception {
        Parameter param = method.getParameters()[0];
        BPMVariable varAnnotation = param.getAnnotation(BPMVariable.class);
        
        return List.of(WorkerMethod.ParameterInfo.builder()
                .parameter(param)
                .variableName("param1")
                .type(String.class)
                .required(false)
                .defaultValue("")
                .variableAnnotation(varAnnotation)
                .build());
    }

    private WorkerMethod createWorkerMethodWithResultAnnotation() throws Exception {
        Method method = TestWorker.class.getMethod("processTaskWithResult", String.class);
        BPMResult resultAnnotation = mock(BPMResult.class);
        when(resultAnnotation.name()).thenReturn("result");
        when(resultAnnotation.flatten()).thenReturn(false);
        
        return WorkerMethod.builder()
                .bean(testWorker)
                .method(method)
                .parameters(createParameterInfos(method))
                .throwsExceptionMappings(new HashMap<>())
                .resultAnnotation(resultAnnotation)
                .build();
    }

    private WorkerMethod createWorkerMethodThatThrows() throws Exception {
        Method method = TestWorker.class.getMethod("processTaskThatThrows", String.class);
        BPMError errorAnnotation = mock(BPMError.class);
        when(errorAnnotation.errorCode()).thenReturn("TEST_ERROR");
        
        Map<Class<?>, WorkerMethod.ThrowsExceptionInfo> exceptionMappings = new HashMap<>();
        exceptionMappings.put(Exception.class, WorkerMethod.ThrowsExceptionInfo.builder()
                .errorCode("TEST_ERROR")
                .errorMessage("Test error")
                .build());
        
        return WorkerMethod.builder()
                .bean(testWorker)
                .method(method)
                .parameters(createParameterInfos(method))
                .throwsExceptionMappings(exceptionMappings)
                .build();
    }

    private WorkerMethod createWorkerMethodThatThrowsRuntimeException() throws Exception {
        Method method = TestWorker.class.getMethod("processTaskThatThrowsRuntimeException", String.class);
        return WorkerMethod.builder()
                .bean(testWorker)
                .method(method)
                .parameters(createParameterInfos(method))
                .throwsExceptionMappings(new HashMap<>())
                .build();
    }

    // Test worker implementation
    public static class TestWorker {
        private String lastResult;

        public Map<String, Object> processTask(String param) {
            lastResult = "processed-" + param;
            return Map.of("result", lastResult);
        }

        @BPMResult
        public Map<String, Object> processTaskWithResult(String param) {
            lastResult = "processed-" + param;
            return Map.of("resultKey", "resultValue");
        }

        @BPMError(errorCode = "TEST_ERROR")
        public Map<String, Object> processTaskThatThrows(String param) throws Exception {
            throw new Exception("Test exception");
        }

        public Map<String, Object> processTaskThatThrowsRuntimeException(String param) {
            throw new RuntimeException("Test runtime exception");
        }

        public String getLastResult() {
            return lastResult;
        }
    }
}
