package com.sasieczno.homeheat.manager.repository.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalTime;

@Configuration
public class JacksonConfiguration {

    @Bean("yamlConfigObjectMapper")
    public ObjectMapper getHeatControllerObjectMapper() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory().enable(YAMLGenerator.Feature.MINIMIZE_QUOTES));
        mapper.findAndRegisterModules();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer());
        simpleModule.addSerializer(LocalTime.class, new LocalTimeSerializer());
        mapper.registerModule(simpleModule);
        return mapper;
    }
}
