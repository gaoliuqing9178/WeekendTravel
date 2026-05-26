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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PlanControllerCreateTests {

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createReturnsAcceptedPlanIdAndProcessingStatus() throws Exception {
        HttpResponse<String> response = postPlan("""
                {
                  "text": "今天下午是空的，想和老婆孩子出去玩几个小时",
                  "scenario": "family",
                  "origin": "当前位置"
                }
                """);

        assertEquals(202, response.statusCode());
        JsonNode json = objectMapper.readTree(response.body());
        assertFalse(json.path("planId").asText().isBlank(), response.body());
        assertEquals("processing", json.path("status").asText());
    }

    @Test
    void createAllowsConfiguredFrontendOrigins() throws Exception {
        for (String origin : new String[]{"http://localhost:5173", "http://127.0.0.1:5173"}) {
            HttpResponse<String> response = postPlan("""
                    {
                      "text": "今天下午想出去玩",
                      "scenario": "family"
                    }
                    """, origin);

            assertEquals(202, response.statusCode());
            assertEquals(origin, response.headers().firstValue("Access-Control-Allow-Origin").orElse(""));
        }
    }

    @Test
    void createRejectsMissingText() throws Exception {
        HttpResponse<String> response = postPlan("""
                {
                  "scenario": "family",
                  "origin": "当前位置"
                }
                """);

        assertInvalidInput(response, "text is required", "text");
    }

    @Test
    void createRejectsBlankText() throws Exception {
        HttpResponse<String> response = postPlan("""
                {
                  "text": "   ",
                  "scenario": "family"
                }
                """);

        assertInvalidInput(response, "text is required", "text");
    }

    @Test
    void createRejectsMissingScenario() throws Exception {
        HttpResponse<String> response = postPlan("""
                {
                  "text": "今天下午想出去玩"
                }
                """);

        assertInvalidInput(response, "scenario is required", "scenario");
    }

    @Test
    void createRejectsInvalidScenario() throws Exception {
        HttpResponse<String> response = postPlan("""
                {
                  "text": "今天下午想出去玩",
                  "scenario": "couple"
                }
                """);

        assertInvalidInput(response, "scenario must be family or friends", null);
    }

    @Test
    void createRejectsEmptyBody() throws Exception {
        HttpResponse<String> response = postPlan("");

        assertEquals(400, response.statusCode());
        JsonNode json = objectMapper.readTree(response.body());
        assertEquals("INVALID_INPUT", json.path("error").asText());
        assertEquals("request body is required", json.path("message").asText());
    }

    private HttpResponse<String> postPlan(String body) throws Exception {
        return postPlan(body, null);
    }

    private HttpResponse<String> postPlan(String body, String origin) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/api/plan"))
                .header("Content-Type", "application/json");

        if (origin != null) {
            request.header("Origin", origin);
        }

        return httpClient.send(
                request.POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
    }

    private void assertInvalidInput(HttpResponse<String> response, String message, String field) throws Exception {
        assertEquals(400, response.statusCode());
        JsonNode json = objectMapper.readTree(response.body());
        assertEquals("INVALID_INPUT", json.path("error").asText());
        assertEquals(message, json.path("message").asText());
        if (field == null) {
            assertEquals(true, json.path("details").isMissingNode() || json.path("details").isNull());
            return;
        }
        assertEquals(field, json.path("details").path("field").asText());
    }
}
