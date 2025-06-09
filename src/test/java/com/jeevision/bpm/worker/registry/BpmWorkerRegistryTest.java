package com.jeevision.bpm.worker.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import com.jeevision.bpm.worker.annotation.BpmResult;
import com.jeevision.bpm.worker.annotation.BpmVariable;
import com.jeevision.bpm.worker.annotation.BpmWorker;
import com.jeevision.bpm.worker.model.WorkerMethod;

class BpmWorkerRegistryTest {

    private BpmWorkerRegistry registry;
    private ApplicationContext mockContext;
    private ExpressionParser mockParser;

    @BeforeEach
    void setUp() {
        mockContext = Mockito.mock(ApplicationContext.class);
        mockParser = Mockito.mock(SpelExpressionParser.class);
        registry = new BpmWorkerRegistry(mockContext);
        
        // Use reflection to inject mock parser
        try {
            Field parserField = BpmWorkerRegistry.class.getDeclaredField("expressionParser");
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
            @BpmWorker("#{'test-topic'}")
            @BpmResult
            public Map<String, Object> processTask(
                    @BpmVariable("#{'test-var'}") String param) {
                return Map.of("result", "success");
            }
        }
        
        TestWorker worker = new TestWorker();
        
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
