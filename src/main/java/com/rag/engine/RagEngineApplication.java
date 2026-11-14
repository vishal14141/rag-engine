package com.rag.engine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RagEngineApplication {

	public static void main(String[] args) {
		SpringApplication.run(RagEngineApplication.class, args);
	}

}
