package com.jeevision.bpm.worker.registry;

import com.jeevision.bpm.worker.annotation.BPMVariable;
import com.jeevision.bpm.worker.annotation.BPMWorker;
import com.jeevision.bpm.worker.model.WorkerMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class BPMWorkerRegistryTest {

    private BPMWorkerRegistry registry;
    private ApplicationContext mockContext;
    private ExpressionParser mockParser;

    @BeforeEach
    void setUp() {
        mockContext = Mockito.mock(ApplicationContext.class);
        mockParser = Mockito.mock(SpelExpressionParser.class);
        registry = new BPMWorkerRegistry(mockContext);
        
        // Use reflection to inject mock parser
        try {
            Field parserField = BPMWorkerRegistry.class.getDeclaredField("expressionParser");
            parserField.setAccessible(true);
            parserField.set(registry, mockParser);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mock parser", e);
        }
    }

    @Test
    void testSpelIntegration_ThroughPublicMethods() throws Exception {
        // Setup test worker class
        class TestWorker {
            @BPMWorker("#{'test-topic'}")
            @BPMResult
            public Map<String, Object> processTask(
                    @BPMVariable("#{'test-var'}") String param) {
                return Map.of("result", "success");
            }
        }
        
        TestWorker worker = new TestWorker();
        Method method = worker.getClass().getMethod("processTask", String.class);
        
        // Mock expression evaluation
        Expression mockExpression = Mockito.mock(Expression.class);
        when(mockParser.parseExpression(anyString())).thenReturn(mockExpression);
        when(mockExpression.getValue(any(StandardEvaluationContext.class)))
            .thenReturn("resolved-topic")  // For topic
            .thenReturn("resolved-var");   // For variable
        
        // Test through public registration flow
        registry.postProcessAfterInitialization(worker, "testWorker");
        
        // Verify topic resolution
        WorkerMethod workerMethod = registry.getWorkerMethod("resolved-topic").orElseThrow();
        assertEquals("resolved-topic", workerMethod.getTopic());
        
        // Verify variable name resolution
        WorkerMethod.ParameterInfo paramInfo = workerMethod.getParameters().get(0);
        assertEquals("resolved-var", paramInfo.getVariableName());
    }
}
