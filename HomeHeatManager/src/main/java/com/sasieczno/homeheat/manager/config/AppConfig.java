package com.sasieczno.homeheat.manager.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
@EnableConfigurationProperties
@PropertySource(value="classpath:application.yml")
@EnableScheduling
public class AppConfig {

    @Value("${manager.version}")
    public String applicationVersion;

    @Value("${manager.udp.port}")
    public Integer udpManagerPort;

    @Value("${manager.udp.timeout}")
    public Integer udpManagerTimeout;

    @Value("${controller.configFile}")
    public String controllerConfigFile;

    @Value("${manager.token.expiry}")
    public Long managerTokenExpiry = 180000L;

    @Value("${manager.refreshToken.expiry}")
    public Long managerRefreshTokenExpiry = 18000000L;

    @Value("${manager.token.secret}")
    public String managerTokenSecret;

    @Value("${manager.admin.password}")
    public String adminPassword;

    @Value("${manager.user.password}")
    public String userPassword;

    @Value("${manager.token.expiredRemovalPeriod}")
    public Long managerTokenExpiredRemovalPeriod = 60000L;

    @Bean
    @Primary
    public ObjectMapper getObjectMapper() {
        return new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .findAndRegisterModules();
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
