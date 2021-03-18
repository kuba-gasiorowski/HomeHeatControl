package com.sasieczno.homeheat.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@SpringBootApplication
public class ManagerApplication {

	public static void main(String[] args) {

		SpringApplication.run(ManagerApplication.class, args);
	}

	@Bean
	@Primary
	public ObjectMapper getObjectMapper() {
		return new ObjectMapper();
	}
}
