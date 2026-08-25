package com.jeevision.bpm.worker.config;

import org.cibseven.bpm.client.spi.DataFormatConfigurator;
import org.cibseven.bpm.client.variable.impl.format.json.JacksonJsonDataFormat;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Configures the Camunda external task client's internal ObjectMapper with
 * JavaTimeModule to support Java 8 date/time types (LocalDateTime, etc.)
 * during variable serialization.
 *
 * @author Slava Yermakov
 */
public class JacksonJsonDataFormatConfigurator implements DataFormatConfigurator<JacksonJsonDataFormat> {

    @Override
    public Class<JacksonJsonDataFormat> getDataFormatClass() {
        return JacksonJsonDataFormat.class;
    }

    @Override
    public void configure(JacksonJsonDataFormat dataFormat) {
        dataFormat.getObjectMapper().registerModule(new JavaTimeModule());
    }
}