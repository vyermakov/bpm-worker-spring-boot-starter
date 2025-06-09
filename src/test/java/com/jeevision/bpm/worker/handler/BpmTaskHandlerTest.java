package com.jeevision.bpm.worker.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jeevision.bpm.worker.annotation.BpmError;
import com.jeevision.bpm.worker.annotation.BpmResult;
import com.jeevision.bpm.worker.annotation.BpmVariable;
import com.jeevision.bpm.worker.model.WorkerMethod;

@ExtendWith(MockitoExtension.class)
class BpmTaskHandlerTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ExternalTask externalTask;

    @Mock
    private ExternalTaskService externalTaskService;

    @Mock
    private WorkerMethod workerMethod;

    private BpmTaskHandler taskHandler;

    @BeforeEach
    void setUp() {
        taskHandler = new BpmTaskHandler(objectMapper);
    }

    @Test
    void testExecute_SuccessfulExecution() throws Exception {
        // Arrange
        String taskId = "task-123";
        String topicName = "test-topic";
        Map<String, Object> variables = Map.of("input", "test-value");
        Map<String, Object> resultVariables = Map.of("output", "result-value");

        when(externalTask.getId()).thenReturn(taskId);
        when(externalTask.getTopicName()).thenReturn(topicName);
        when(externalTask.getAllVariables()).thenReturn(variables);

        Object mockBean = new TestWorker();
        Method mockMethod = TestWorker.class.getMethod("processTask", String.class);
        
        WorkerMethod.ParameterInfo paramInfo = WorkerMethod.ParameterInfo.builder()
                .parameter(mockMethod.getParameters()[0])
                .variableName("input")
                .type(String.class)
                .required(false)
                .defaultValue("")
                .build();

        BpmResult resultAnnotation = mock(BpmResult.class);
        when(resultAnnotation.name()).thenReturn("output");
        when(resultAnnotation.flatten()).thenReturn(false);

        when(workerMethod.getBean()).thenReturn(mockBean);
        when(workerMethod.getMethod()).thenReturn(mockMethod);
        when(workerMethod.getParameters()).thenReturn(List.of(paramInfo));
        when(workerMethod.getResultAnnotation()).thenReturn(resultAnnotation);

        taskHandler = taskHandler.withWorkerMethod(workerMethod);

        // Act
        taskHandler.execute(externalTask, externalTaskService);

        // Assert
        verify(externalTaskService).complete(eq(externalTask), any());
        verify(externalTaskService, never()).handleBpmnError(any(ExternalTask.class), any(), any(), any());
        verify(externalTaskService, never()).handleFailure(any(ExternalTask.class), any(), any(), anyInt(), anyLong());
    }

    @Test
    void testExecute_WithBpmnError() throws Exception {
        // Arrange
        String taskId = "task-123";
        String topicName = "test-topic";
        Map<String, Object> variables = Map.of("input", "error-trigger");

        when(externalTask.getId()).thenReturn(taskId);
        when(externalTask.getTopicName()).thenReturn(topicName);
        when(externalTask.getAllVariables()).thenReturn(variables);

        Object mockBean = new TestWorkerWithError();
        Method mockMethod = TestWorkerWithError.class.getMethod("processTaskWithError", String.class);
        
        WorkerMethod.ParameterInfo paramInfo = WorkerMethod.ParameterInfo.builder()
                .parameter(mockMethod.getParameters()[0])
                .variableName("input")
                .type(String.class)
                .required(false)
                .defaultValue("")
                .build();

        BpmError bpmError = mockMethod.getAnnotation(BpmError.class);
        
        WorkerMethod.ThrowsExceptionInfo throwsInfo = WorkerMethod.ThrowsExceptionInfo.builder()
                .exceptionType(IllegalArgumentException.class)
                .bmpErrorAnnotation(bpmError)
                .errorCode("BUSINESS_ERROR")
                .errorMessage("Business validation failed")
                .build();

        when(workerMethod.getBean()).thenReturn(mockBean);
        when(workerMethod.getMethod()).thenReturn(mockMethod);
        when(workerMethod.getParameters()).thenReturn(List.of(paramInfo));
        when(workerMethod.getThrowsExceptionMappings()).thenReturn(Map.of(IllegalArgumentException.class, throwsInfo));

        taskHandler = taskHandler.withWorkerMethod(workerMethod);

        // Act
        taskHandler.execute(externalTask, externalTaskService);

        // Assert
        verify(externalTaskService).handleBpmnError(eq(externalTask), eq("BUSINESS_ERROR"), eq("Business validation failed"), any());
        verify(externalTaskService, never()).complete(any(), any());
        verify(externalTaskService, never()).handleFailure(any(ExternalTask.class), any(), any(), anyInt(), anyLong());
    }

    @Test
    void testExecute_WithTechnicalFailure() throws Exception {
        // Arrange
        String taskId = "task-123";
        String topicName = "test-topic";
        Map<String, Object> variables = Map.of("input", "runtime-error");

        when(externalTask.getId()).thenReturn(taskId);
        when(externalTask.getTopicName()).thenReturn(topicName);
        when(externalTask.getAllVariables()).thenReturn(variables);

        Object mockBean = new TestWorkerWithRuntimeError();
        Method mockMethod = TestWorkerWithRuntimeError.class.getMethod("processTaskWithRuntimeError", String.class);
        
        WorkerMethod.ParameterInfo paramInfo = WorkerMethod.ParameterInfo.builder()
                .parameter(mockMethod.getParameters()[0])
                .variableName("input")
                .type(String.class)
                .required(false)
                .defaultValue("")
                .build();

        when(workerMethod.getBean()).thenReturn(mockBean);
        when(workerMethod.getMethod()).thenReturn(mockMethod);
        when(workerMethod.getParameters()).thenReturn(List.of(paramInfo));
        when(workerMethod.getThrowsExceptionMappings()).thenReturn(Map.of());

        taskHandler = taskHandler.withWorkerMethod(workerMethod);

        // Act
        taskHandler.execute(externalTask, externalTaskService);

        // Assert
        verify(externalTaskService).handleFailure(eq(externalTask), eq("Runtime error occurred"), anyString(), eq(3), eq(10000L));
        verify(externalTaskService, never()).complete(any(), any());
        verify(externalTaskService, never()).handleBpmnError(any(ExternalTask.class), any(), any(), any());
    }

    @Test
    void testExecute_WithExternalTaskParameter() throws Exception {
        // Arrange
        String taskId = "task-123";
        String topicName = "test-topic";
        Map<String, Object> variables = Map.of("input", "test-value");

        when(externalTask.getId()).thenReturn(taskId);
        when(externalTask.getTopicName()).thenReturn(topicName);
        when(externalTask.getAllVariables()).thenReturn(variables);

        Object mockBean = new TestWorkerWithExternalTask();
        Method mockMethod = TestWorkerWithExternalTask.class.getMethod("processTaskWithExternalTask", ExternalTask.class, String.class);
        
        WorkerMethod.ParameterInfo paramInfo1 = WorkerMethod.ParameterInfo.builder()
                .parameter(mockMethod.getParameters()[0])
                .variableName("externalTask")
                .type(ExternalTask.class)
                .required(false)
                .defaultValue("")
                .build();

        WorkerMethod.ParameterInfo paramInfo2 = WorkerMethod.ParameterInfo.builder()
                .parameter(mockMethod.getParameters()[1])
                .variableName("input")
                .type(String.class)
                .required(false)
                .defaultValue("")
                .build();

        when(workerMethod.getBean()).thenReturn(mockBean);
        when(workerMethod.getMethod()).thenReturn(mockMethod);
        when(workerMethod.getParameters()).thenReturn(List.of(paramInfo1, paramInfo2));
        when(workerMethod.getResultAnnotation()).thenReturn(null);

        taskHandler = taskHandler.withWorkerMethod(workerMethod);

        // Act
        taskHandler.execute(externalTask, externalTaskService);

        // Assert
        verify(externalTaskService).complete(eq(externalTask), any());
    }

    @Test
    void testExecute_WithNullResult() throws Exception {
        // Arrange
        String taskId = "task-123";
        String topicName = "test-topic";
        Map<String, Object> variables = Map.of("input", "null-result");

        when(externalTask.getId()).thenReturn(taskId);
        when(externalTask.getTopicName()).thenReturn(topicName);
        when(externalTask.getAllVariables()).thenReturn(variables);

        Object mockBean = new TestWorkerWithNullResult();
        Method mockMethod = TestWorkerWithNullResult.class.getMethod("processTaskWithNullResult", String.class);
        
        WorkerMethod.ParameterInfo paramInfo = WorkerMethod.ParameterInfo.builder()
                .parameter(mockMethod.getParameters()[0])
                .variableName("input")
                .type(String.class)
                .required(false)
                .defaultValue("")
                .build();

        BpmResult resultAnnotation = mock(BpmResult.class);
        when(resultAnnotation.name()).thenReturn("output");
        when(resultAnnotation.nullHandling()).thenReturn(BpmResult.NullHandling.SET_NULL);

        when(workerMethod.getBean()).thenReturn(mockBean);
        when(workerMethod.getMethod()).thenReturn(mockMethod);
        when(workerMethod.getParameters()).thenReturn(List.of(paramInfo));
        when(workerMethod.getResultAnnotation()).thenReturn(resultAnnotation);

        taskHandler = taskHandler.withWorkerMethod(workerMethod);

        // Act
        taskHandler.execute(externalTask, externalTaskService);

        // Assert
        verify(externalTaskService).complete(eq(externalTask), argThat(map -> 
            map.containsKey("output") && map.get("output") == null));
    }

    @Test
    void testExecute_WithFlattenedResult() throws Exception {
        // Arrange
        String taskId = "task-123";
        String topicName = "test-topic";
        Map<String, Object> variables = Map.of("input", "flatten-result");

        when(externalTask.getId()).thenReturn(taskId);
        when(externalTask.getTopicName()).thenReturn(topicName);
        when(externalTask.getAllVariables()).thenReturn(variables);

        Object mockBean = new TestWorkerWithFlattenedResult();
        Method mockMethod = TestWorkerWithFlattenedResult.class.getMethod("processTaskWithFlattenedResult", String.class);
        
        WorkerMethod.ParameterInfo paramInfo = WorkerMethod.ParameterInfo.builder()
                .parameter(mockMethod.getParameters()[0])
                .variableName("input")
                .type(String.class)
                .required(false)
                .defaultValue("")
                .build();

        BpmResult resultAnnotation = mock(BpmResult.class);
        when(resultAnnotation.flatten()).thenReturn(true);
        when(resultAnnotation.includeNullProperties()).thenReturn(false);

        Map<String, Object> flattenedMap = Map.of("key1", "value1", "key2", "value2");
        when(objectMapper.convertValue(any(), eq(Map.class))).thenReturn((Map) flattenedMap);

        when(workerMethod.getBean()).thenReturn(mockBean);
        when(workerMethod.getMethod()).thenReturn(mockMethod);
        when(workerMethod.getParameters()).thenReturn(List.of(paramInfo));
        when(workerMethod.getResultAnnotation()).thenReturn(resultAnnotation);

        taskHandler = taskHandler.withWorkerMethod(workerMethod);

        // Act
        taskHandler.execute(externalTask, externalTaskService);

        // Assert
        verify(externalTaskService).complete(eq(externalTask), argThat(map -> 
            map.containsKey("key1") && map.containsKey("key2")));
    }

    @Test
    void testExecute_WithTypeConversion() throws Exception {
        // Arrange
        String taskId = "task-123";
        String topicName = "test-topic";
        Map<String, Object> variables = Map.of("numberInput", "123");

        when(externalTask.getId()).thenReturn(taskId);
        when(externalTask.getTopicName()).thenReturn(topicName);
        when(externalTask.getAllVariables()).thenReturn(variables);

        Object mockBean = new TestWorkerWithTypeConversion();
        Method mockMethod = TestWorkerWithTypeConversion.class.getMethod("processTaskWithTypeConversion", Integer.class);
        
        WorkerMethod.ParameterInfo paramInfo = WorkerMethod.ParameterInfo.builder()
                .parameter(mockMethod.getParameters()[0])
                .variableName("numberInput")
                .type(Integer.class)
                .required(false)
                .defaultValue("")
                .build();

        when(objectMapper.convertValue("123", Integer.class)).thenReturn(123);

        when(workerMethod.getBean()).thenReturn(mockBean);
        when(workerMethod.getMethod()).thenReturn(mockMethod);
        when(workerMethod.getParameters()).thenReturn(List.of(paramInfo));
        when(workerMethod.getResultAnnotation()).thenReturn(null);

        taskHandler = taskHandler.withWorkerMethod(workerMethod);

        // Act
        taskHandler.execute(externalTask, externalTaskService);

        // Assert
        verify(externalTaskService).complete(eq(externalTask), any());
        verify(objectMapper).convertValue("123", Integer.class);
    }

    // Test worker classes
    public static class TestWorker {
        public String processTask(@BpmVariable("input") String input) {
            return "processed: " + input;
        }
    }

    public static class TestWorkerWithError {
        @BpmError(errorCode = "DEFAULT_ERROR", errorMappings = {
            @BpmError.ErrorMapping(exception = IllegalArgumentException.class, errorCode = "BUSINESS_ERROR", errorMessage = "Business validation failed")
        })
        public String processTaskWithError(@BpmVariable("input") String input) {
            if ("error-trigger".equals(input)) {
                throw new IllegalArgumentException("Business validation failed");
            }
            return "processed: " + input;
        }
    }

    public static class TestWorkerWithRuntimeError {
        public String processTaskWithRuntimeError(@BpmVariable("input") String input) {
            if ("runtime-error".equals(input)) {
                throw new RuntimeException("Runtime error occurred");
            }
            return "processed: " + input;
        }
    }

    public static class TestWorkerWithExternalTask {
        public String processTaskWithExternalTask(ExternalTask externalTask, @BpmVariable("input") String input) {
            return "processed: " + input + " for task: " + externalTask.getId();
        }
    }

    public static class TestWorkerWithNullResult {
        public String processTaskWithNullResult(@BpmVariable("input") String input) {
            return null;
        }
    }

    public static class TestWorkerWithFlattenedResult {
        public Map<String, Object> processTaskWithFlattenedResult(@BpmVariable("input") String input) {
            Map<String, Object> result = new HashMap<>();
            result.put("key1", "value1");
            result.put("key2", "value2");
            return result;
        }
    }

    public static class TestWorkerWithTypeConversion {
        public String processTaskWithTypeConversion(@BpmVariable("numberInput") Integer number) {
            return "processed number: " + number;
        }
    }
}
