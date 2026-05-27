package com.weekendtravel.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PlanControllerIntegrationTests {

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createdPlanCanOpenStreamAndReceiveStateChangeForSamePlanId() throws Exception {
        HttpResponse<String> createResponse = postPlan("""
                {
                  "text": "今天下午是空的，想和老婆孩子出去玩几个小时",
                  "scenario": "family",
                  "origin": "当前位置"
                }
                """);

        assertEquals(202, createResponse.statusCode());
        JsonNode createJson = objectMapper.readTree(createResponse.body());
        String planId = createJson.path("planId").asText();
        assertFalse(planId.isBlank(), createResponse.body());
        assertEquals("processing", createJson.path("status").asText());

        HttpResponse<String> streamResponse = openStream(planId);

        assertEquals(200, streamResponse.statusCode());
        String contentType = streamResponse.headers().firstValue("content-type").orElse("");
        assertTrue(contentType.contains("text/event-stream"), streamResponse.headers().map().toString());
        String bodyText = streamResponse.body();
        assertTrue(bodyText.contains("event:heartbeat"), bodyText);
        assertTrue(bodyText.contains("event:state_change"), bodyText);
        assertTrue(bodyText.contains("\"type\":\"state_change\""), bodyText);
        assertTrue(bodyText.contains("\"planId\":\"" + planId + "\""), bodyText);
        assertTrue(bodyText.contains("\"from\":\"START\""), bodyText);
        assertTrue(bodyText.contains("\"to\":\"INTENT\""), bodyText);
    }

    private HttpResponse<String> postPlan(String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/api/plan"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(5))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        return httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
    }

    private HttpResponse<String> openStream(String planId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/api/plan/" + planId + "/stream"))
                .header("Accept", "text/event-stream")
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        return httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
    }
}
