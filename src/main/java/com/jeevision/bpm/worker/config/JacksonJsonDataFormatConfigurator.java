package com.jeevision.bpm.worker.config;

import org.operaton.bpm.client.spi.DataFormatConfigurator;
import org.operaton.bpm.client.variable.impl.format.json.JacksonJsonDataFormat;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Configures Operaton external task client's JSON serialization
 * to use Spring's ObjectMapper with all its settings (date format, etc.).
 *
 * @author Slava Yermakov
 */
public class JacksonJsonDataFormatConfigurator implements DataFormatConfigurator<JacksonJsonDataFormat> {

    private static ObjectMapper springObjectMapper;

    /**
     * Should be called before {@code ExternalTaskClientBuilder.build()}
     * to apply Spring's ObjectMapper settings.
     */
    public static void setObjectMapper(ObjectMapper objectMapper) {
        springObjectMapper = objectMapper;
    }

    @Override
    public Class<JacksonJsonDataFormat> getDataFormatClass() {
        return JacksonJsonDataFormat.class;
    }

    @Override
    public void configure(JacksonJsonDataFormat dataFormat) {
        if (springObjectMapper != null) {
            dataFormat.setObjectMapper(springObjectMapper);
        }
    }
}