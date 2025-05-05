package com.sasieczno.homeheat.manager.repository.impl;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactoryBuilder;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.sasieczno.homeheat.manager.model.CircuitMode;
import com.sasieczno.homeheat.manager.model.CircuitStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Configuration
public class JacksonConfiguration {

    @Bean("yamlConfigObjectMapper")
    public ObjectMapper getHeatControllerObjectMapper() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactoryBuilder(new YAMLFactory())
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                .enable(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR)
                .stringQuotingChecker(new StringQuotingChecker())
                .build());
        mapper.findAndRegisterModules();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        SimpleModule simpleModule = new SimpleModule()
                .addDeserializer(LocalTime.class, new LocalTimeDeserializer())
                .addDeserializer(CircuitMode.class, new CircuitStatus.CircuitStatusDeserializer())
                .addSerializer(Double.class, new DoubleSerializer());
        mapper.registerModule(simpleModule);
        return mapper;
    }
}
