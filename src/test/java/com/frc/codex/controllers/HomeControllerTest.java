package com.frc.codex.controllers;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
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
				String.class)).contains("UK Filing Index");
	}

	@Test
	void notFoundPage() {
		assertThat(this.restTemplate.getForEntity("http://localhost:" + port + "/nonexistent",
				String.class).getStatusCode()).isEqualTo(HttpStatusCode.valueOf(404));
	}

}
