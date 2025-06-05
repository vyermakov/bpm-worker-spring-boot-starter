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
        registry = new BPMWorkerRegistry(mockContext) {
            @Override
            protected ExpressionParser createExpressionParser() {
                return mockParser;
            }
        };
    }

    @Test
    void testEvaluateSpelExpression_PlainString() {
        String result = registry.evaluateSpelExpression("plain-string");
        assertEquals("plain-string", result);
    }

    @Test
    void testEvaluateSpelExpression_SpelExpression() throws Exception {
        Expression mockExpression = Mockito.mock(Expression.class);
        when(mockParser.parseExpression(anyString())).thenReturn(mockExpression);
        when(mockExpression.getValue(any(StandardEvaluationContext.class))).thenReturn("resolved-value");

        String result = registry.evaluateSpelExpression("#{'test-expression'}");
        assertEquals("resolved-value", result);
    }

    @Test
    void testEvaluateSpelExpression_InvalidExpression() throws Exception {
        when(mockParser.parseExpression(anyString())).thenThrow(new RuntimeException("Invalid expression"));

        String result = registry.evaluateSpelExpression("#{'invalid'}");
        assertEquals("#{'invalid'}", result);
    }

    @Test
    void testDetermineTopic_WithSpelExpression() throws Exception {
        Expression mockExpression = Mockito.mock(Expression.class);
        when(mockParser.parseExpression(anyString())).thenReturn(mockExpression);
        when(mockExpression.getValue(any(StandardEvaluationContext.class))).thenReturn("resolved-topic");

        BPMWorker workerAnnotation = Mockito.mock(BPMWorker.class);
        when(workerAnnotation.value()).thenReturn("");
        when(workerAnnotation.topic()).thenReturn("#{'test-topic'}");

        String topic = registry.determineTopic(workerAnnotation);
        assertEquals("resolved-topic", topic);
    }

    @Test
    void testDetermineVariableName_WithSpelExpression() throws Exception {
        Expression mockExpression = Mockito.mock(Expression.class);
        when(mockParser.parseExpression(anyString())).thenReturn(mockExpression);
        when(mockExpression.getValue(any(StandardEvaluationContext.class))).thenReturn("resolved-variable");

        Parameter mockParameter = Mockito.mock(Parameter.class);
        when(mockParameter.getName()).thenReturn("defaultName");

        BPMVariable mockAnnotation = Mockito.mock(BPMVariable.class);
        when(mockAnnotation.value()).thenReturn("#{'test-variable'}");

        String variableName = registry.determineVariableName(mockParameter, mockAnnotation);
        assertEquals("resolved-variable", variableName);
    }

    @Test
    void testDetermineVariableName_WithoutAnnotation() throws Exception {
        Parameter mockParameter = Mockito.mock(Parameter.class);
        when(mockParameter.getName()).thenReturn("defaultName");

        String variableName = registry.determineVariableName(mockParameter, null);
        assertEquals("defaultName", variableName);
    }
}
