package com.jeevision.bpm.worker.handler;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.Collections;
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
import com.jeevision.bpm.worker.annotation.BPMError;
import com.jeevision.bpm.worker.annotation.BPMResult;
import com.jeevision.bpm.worker.model.WorkerMethod;

@ExtendWith(MockitoExtension.class)
class BPMTaskHandlerTest {

	private static class TestWorker {
		@BPMResult(name = "result")
		public String successfulMethod(String input) {
			return "Success: " + input;
		}

		@BPMError(errorCode = "TEST_ERROR", errorMessage = "Test error message")
		public void methodWithBpmnError() {
			throw new RuntimeException("Test error");
		}

		public void methodWithTechnicalError() {
			throw new IllegalStateException("Technical error");
		}
	}
	
	public static class Parameter {
	    private final String name;
	    private final Class<?> type;

	    public Parameter(String name, Class<?> type) {
	        this.name = name;
	        this.type = type;
	    }
	}

	public static class Result {
	    private final String name;
	    private final Class<?> type;

	    public Result(String name, Class<?> type) {
	        this.name = name;
	        this.type = type;
	    }
	}
	
	@Mock
	private ExternalTask externalTask;

	@Mock
	private ExternalTaskService externalTaskService;

	private BPMTaskHandler taskHandler;
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper();
		taskHandler = new BPMTaskHandler(objectMapper);
	}

	@Test
	void shouldHandleSuccessfulExecution() throws Exception {
		// Given
		TestWorker testWorker = new TestWorker();
		Method method = TestWorker.class.getMethod("successfulMethod", String.class);
		WorkerMethod workerMethod = WorkerMethod.builder().bean(testWorker).method(method).topic("testTopic")
				.parameters(Collections.singletonList(WorkerMethod.ParameterInfo.builder()
						.parameter(method.getParameters()[0]).variableName("input").type(String.class).build()))
				.resultAnnotation(method.getAnnotation(BPMResult.class)).build();

		when(externalTask.getAllVariables()).thenReturn(Map.of("input", "test"));
		when(externalTask.getId()).thenReturn("testId");
		when(externalTask.getTopicName()).thenReturn("testTopic");

		// When
		taskHandler.withWorkerMethod(workerMethod).execute(externalTask, externalTaskService);

		// Then
		verify(externalTaskService).complete(eq(externalTask), anyMap());
	}

	@Test
	void shouldHandleBpmnError() throws Exception {
		// Given
		TestWorker testWorker = new TestWorker();
		Method method = TestWorker.class.getMethod("methodWithBpmnError");
		WorkerMethod workerMethod = WorkerMethod.builder().bean(testWorker).method(method).topic("testTopic")
				.parameters(Collections.emptyList()).build();

		when(externalTask.getId()).thenReturn("testId");
		when(externalTask.getTopicName()).thenReturn("testTopic");
		when(externalTask.getAllVariables()).thenReturn(Collections.emptyMap());

		// When
		taskHandler.withWorkerMethod(workerMethod).execute(externalTask, externalTaskService);

		// Then
		verify(externalTaskService).handleBpmnError(eq(externalTask), eq("TEST_ERROR"), eq("Test error message"),
				anyMap());
	}

	@Test
	void shouldHandleTechnicalFailure() throws Exception {
		// Given
		TestWorker testWorker = new TestWorker();
		Method method = TestWorker.class.getMethod("methodWithTechnicalError");
		WorkerMethod workerMethod = WorkerMethod.builder().bean(testWorker).method(method).topic("testTopic")
				.parameters(Collections.emptyList()).build();

		when(externalTask.getId()).thenReturn("testId");
		when(externalTask.getTopicName()).thenReturn("testTopic");
		when(externalTask.getAllVariables()).thenReturn(Collections.emptyMap());

		// When
		taskHandler.withWorkerMethod(workerMethod).execute(externalTask, externalTaskService);

		// Then
		verify(externalTaskService).handleFailure(eq(externalTask), anyString(), anyString(), eq(3), eq(10000L));
	}
}
