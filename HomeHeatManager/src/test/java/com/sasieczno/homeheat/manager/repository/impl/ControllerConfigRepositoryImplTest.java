package com.sasieczno.homeheat.manager.repository.impl;

import com.sasieczno.homeheat.manager.config.AppConfig;
import com.sasieczno.homeheat.manager.model.ControllerConfig;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class ControllerConfigRepositoryImplTest {

    ControllerConfigRepositoryImpl testObj;

    AppConfig cfg;
    JacksonConfiguration jacksonConfiguration;


    @Before
    public void setup() {
        testObj = new ControllerConfigRepositoryImpl();
        cfg = new AppConfig();
        testObj.appConfig = cfg;
        jacksonConfiguration = new JacksonConfiguration();
        testObj.jacksonConfiguration = jacksonConfiguration;
    }

    @Test
    public void test_getConfig() throws Exception {
        ClassLoader cl = this.getClass().getClassLoader();
        cfg.controllerConfigFile = cl.getResource("HomeHeat.yml").getFile();
        ControllerConfig result = testObj.getConfig();
        Assert.assertEquals(-18.0, result.getExtMinTemp(), 0.01);
        Assert.assertEquals(12.0, result.getExtMaxTemp(), 0.01);
        Assert.assertEquals(10, result.getCircuits().size());
        jacksonConfiguration.getHeatControllerObjectMapper().writeValue(System.out, result);
    }
}
