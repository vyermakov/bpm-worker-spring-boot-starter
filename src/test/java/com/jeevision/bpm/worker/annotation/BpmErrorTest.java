package com.jeevision.bpm.worker.annotation;

import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotationUtils;

import static org.assertj.core.api.Assertions.assertThat;

class BpmErrorTest {

    @BpmError("ERROR_CODE")
    static class AnnotatedWithValue {}

    @BpmError(code = "ERROR_CODE")
    static class AnnotatedWithCode {}

    @BpmError(code = "ERROR_CODE", message = "Something went wrong")
    static class AnnotatedWithCodeAndMessage {}

    @Test
    void valueAttributeIsResolvedAsCode() {
        var annotation = AnnotationUtils.findAnnotation(AnnotatedWithValue.class, BpmError.class);
        assertThat(annotation.value()).isEqualTo("ERROR_CODE");
        assertThat(annotation.code()).isEqualTo("ERROR_CODE");
    }

    @Test
    void codeAttributeIsResolvedAsValue() {
        var annotation = AnnotationUtils.findAnnotation(AnnotatedWithCode.class, BpmError.class);
        assertThat(annotation.code()).isEqualTo("ERROR_CODE");
        assertThat(annotation.value()).isEqualTo("ERROR_CODE");
    }

    @Test
    void codeAndValueAreAliases() {
        var byValue = AnnotationUtils.findAnnotation(AnnotatedWithValue.class, BpmError.class);
        var byCode = AnnotationUtils.findAnnotation(AnnotatedWithCode.class, BpmError.class);
        assertThat(byValue.code()).isEqualTo(byCode.value());
    }

    @Test
    void messageIsIndependentFromCodeAlias() {
        var annotation = AnnotationUtils.findAnnotation(AnnotatedWithCodeAndMessage.class, BpmError.class);
        assertThat(annotation.code()).isEqualTo("ERROR_CODE");
        assertThat(annotation.message()).isEqualTo("Something went wrong");
    }

    @Test
    void defaultMessageIsEmpty() {
        var annotation = AnnotationUtils.findAnnotation(AnnotatedWithValue.class, BpmError.class);
        assertThat(annotation.message()).isEmpty();
    }
}
