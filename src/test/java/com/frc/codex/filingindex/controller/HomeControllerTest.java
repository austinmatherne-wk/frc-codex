package com.frc.codex.filingindex.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HomeControllerTest {

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void healthPage() {
		assertThat(this.restTemplate.getForEntity("http://localhost:" + port + "/health",
				String.class).getStatusCode()).isEqualTo(HttpStatusCode.valueOf(200));
	}

	@Test
	void indexPage() {
		assertThat(this.restTemplate.getForObject("http://localhost:" + port + "/",
				String.class)).contains("FRC CODEx Filing Index");
	}

	@Test
	void notFoundPage() {
		assertThat(this.restTemplate.getForObject("http://localhost:" + port + "/nonexistent",
				String.class)).contains("An unexpected error occurred.");
	}

}
