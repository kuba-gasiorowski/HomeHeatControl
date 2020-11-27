package com.sasieczno.homeheat.manager.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@EnableConfigurationProperties
@PropertySource(value="classpath:application.yml")
public class AppConfig {

    @Value("${manager.version}")
    public String applicationVersion;

    @Value("${manager.udp.port}")
    public Integer udpManagerPort;

    @Value("${controller.configFile}")
    public String controllerConfigFile;
}
