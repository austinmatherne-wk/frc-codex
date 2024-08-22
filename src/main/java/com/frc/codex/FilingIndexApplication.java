package com.frc.codex;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(FilingIndexProperties.class)
@EnableScheduling
public class FilingIndexApplication {
	public static void main(String... args) {
		SpringApplication.run(FilingIndexApplication.class, args);
	}
}
