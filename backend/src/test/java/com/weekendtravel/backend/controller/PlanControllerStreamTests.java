package com.weekendtravel.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PlanControllerStreamTests {

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Test
    void streamReturnsEventStreamWithHeartbeatAndStateChange() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/api/plan/test-plan/stream"))
                .header("Accept", "text/event-stream")
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(200, response.statusCode());
        String contentType = response.headers().firstValue("content-type").orElse("");
        assertTrue(contentType.contains("text/event-stream"), response.headers().map().toString());
        assertEquals("no-cache", response.headers().firstValue("cache-control").orElse(""));

        String bodyText = response.body();
        assertTrue(bodyText.contains("heartbeat"), bodyText);
        assertTrue(bodyText.contains("state_change"), bodyText);
        assertTrue(bodyText.contains("test-plan"), bodyText);
        assertTrue(bodyText.contains("START"), bodyText);
        assertTrue(bodyText.contains("INTENT"), bodyText);
    }
}
