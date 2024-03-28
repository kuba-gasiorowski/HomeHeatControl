package com.sasieczno.homeheat.manager.repository.impl;

import com.sasieczno.homeheat.manager.config.AppConfig;
import com.sasieczno.homeheat.manager.model.ControllerConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ControllerConfigRepositoryImplTest {

    ControllerConfigRepositoryImpl testObj;

    AppConfig cfg;
    JacksonConfiguration jacksonConfiguration;


    @BeforeAll
    public void setup() {
        jacksonConfiguration = new JacksonConfiguration();
        cfg = new AppConfig();
        testObj = new ControllerConfigRepositoryImpl(cfg, jacksonConfiguration.getHeatControllerObjectMapper());
    }

    @Test
    public void test_getConfig() throws Exception {
        cfg.controllerConfigFile = "src/test/resources/HomeHeat.yml";
        ControllerConfig result = testObj.getConfig();
        Assertions.assertEquals(-18.0, result.getExtMinTemp(), 0.01);
        Assertions.assertEquals(12.0, result.getExtMaxTemp(), 0.01);
        Assertions.assertEquals(10, result.getCircuits().size());
        jacksonConfiguration.getHeatControllerObjectMapper().writeValue(System.out, result);
    }
}
