package com.sasieczno.homeheat.manager.repository.impl;

import com.sasieczno.homeheat.manager.config.AppConfig;
import com.sasieczno.homeheat.manager.model.ControllerConfig;
import org.assertj.core.internal.Diff;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.LocalTime;

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
        Assertions.assertEquals(LocalTime.of(22, 0, 0), result.getNightStartTime());
        Assertions.assertEquals(LocalTime.of(6, 58, 0), result.getNightEndTime());
        Assertions.assertEquals(LocalTime.of(13, 0, 0), result.getDayStartTime());
        Assertions.assertEquals(LocalTime.of(15, 58, 0), result.getDayEndTime());
        Assertions.assertEquals(2, result.getOffHome().size());
        Assertions.assertEquals(2.0, result.getOffHome().get(0).getDecreaseTemp());
        Assertions.assertEquals(LocalDateTime.of(2024, 2, 3, 11, 13, 15), result.getOffHome().get(0).getDecreaseFrom());
        Assertions.assertEquals(LocalDateTime.of(2024, 2, 8, 17, 18, 31), result.getOffHome().get(0).getDecreaseTo());
        Assertions.assertEquals(8.0, result.getOffHome().get(1).getDecreaseTemp());
        Assertions.assertEquals(LocalDateTime.of(2024, 8, 30, 19, 55, 57), result.getOffHome().get(1).getDecreaseFrom());
        Assertions.assertEquals(LocalDateTime.of(2024, 9, 7, 21, 58, 00), result.getOffHome().get(1).getDecreaseTo());
        String template = new String(getClass().getResource("/HomeHeat2.yml").openStream().readAllBytes());
        String complete = jacksonConfiguration.getHeatControllerObjectMapper().writeValueAsString(result);
//        Assertions.assertEquals(template, complete);
//        Diff diff = new Diff();
//        ByteArrayOutputStream bos = new ByteArrayOutputStream();
//        jacksonConfiguration.getHeatControllerObjectMapper().writeValue(bos, result);
//        diff.diff((new ByteArrayInputStream(bos.toByteArray())), template);
        Assertions.assertLinesMatch(template.lines(), complete.lines());
        //jacksonConfiguration.getHeatControllerObjectMapper().writeValue(System.out, result);
    }
}
