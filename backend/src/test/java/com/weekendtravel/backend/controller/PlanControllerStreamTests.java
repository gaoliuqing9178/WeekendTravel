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
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PlanControllerStreamTests {

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void streamReturnsEventStreamWithStateMachineEvents() throws Exception {
        String planId = createFamilyPlan();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/api/plan/" + planId + "/stream"))
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
        assertTrue(bodyText.contains("tool_call"), bodyText);
        assertTrue(bodyText.contains("tool_result"), bodyText);
        assertTrue(bodyText.contains("plan_ready"), bodyText);
        assertTrue(bodyText.contains(planId), bodyText);
        assertTrue(bodyText.contains("START"), bodyText);
        assertTrue(bodyText.contains("INTENT"), bodyText);
        assertTrue(bodyText.contains("SKELETON"), bodyText);
        assertTrue(bodyText.contains("RECALL"), bodyText);
        assertTrue(bodyText.contains("VALIDATE"), bodyText);
        assertTrue(bodyText.contains("PACK"), bodyText);
        assertTrue(bodyText.contains("\"isPlanB\":"), bodyText);
        assertTrue(bodyText.contains("\"replanCount\":"), bodyText);
    }

    private String createFamilyPlan() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/api/plan"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {
                          "text": "今天下午想带孩子玩一会儿，再找个不用排队太久的家庭餐厅",
                          "scenario": "family",
                          "origin": "当前位置"
                        }
                        """, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        assertEquals(202, response.statusCode());
        JsonNode json = objectMapper.readTree(response.body());
        return json.path("planId").asText();
    }
}
