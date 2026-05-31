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

    @Test
    void clarifyRejectsInvalidState() throws Exception {
        String planId = createPlanId("今天下午想和老婆孩子找个轻松的亲子活动，再吃个不用排太久的晚饭，安排4小时左右", "family");
        HttpResponse<String> response = clarifyPlan(planId, "4-6小时");

        assertEquals(409, response.statusCode());
        JsonNode json = objectMapper.readTree(response.body());
        assertEquals("INVALID_STATE", json.path("error").asText());
        assertEquals("plan is not waiting for clarification", json.path("message").asText());
    }

    @Test
    void adjustRejectsInvalidState() throws Exception {
        String planId = createPlanId("今天下午是空的，想和老婆孩子出去玩几个小时", "family");
        HttpResponse<String> response = adjustPlan(planId, "换一家餐厅，要能订位的");

        assertEquals(409, response.statusCode());
        JsonNode json = objectMapper.readTree(response.body());
        assertEquals("INVALID_STATE", json.path("error").asText());
        assertEquals("plan is not in CONFIRM state before adjust", json.path("message").asText());
    }

    @Test
    void executeRejectsInvalidState() throws Exception {
        String planId = createPlanId("今天下午是空的，想和老婆孩子出去玩几个小时", "family");
        HttpResponse<String> response = executePlan(planId, true);

        assertEquals(409, response.statusCode());
        JsonNode json = objectMapper.readTree(response.body());
        assertEquals("INVALID_STATE", json.path("error").asText());
        assertEquals("plan must be in CONFIRM state before execute", json.path("message").asText());
    }

    @Test
    void executeRejectsUnconfirmedRequest() throws Exception {
        String planId = createPlanId("今天下午是空的，想和老婆孩子出去玩几个小时", "family");
        HttpResponse<String> response = executePlan(planId, false);

        assertEquals(400, response.statusCode());
        JsonNode json = objectMapper.readTree(response.body());
        assertEquals("INVALID_INPUT", json.path("error").asText());
        assertEquals("confirmed must be true", json.path("message").asText());
    }

    @Test
    void adjustRejectsFourthAttempt() throws Exception {
        String planId = createPlanId("今天下午想和老婆孩子找个轻松的亲子活动，再吃个不用排太久的晚饭，安排4小时左右", "family");
        HttpResponse<String> initialStream = streamPlan(planId);
        assertEquals(200, initialStream.statusCode());

        for (int index = 0; index < 3; index++) {
            HttpResponse<String> adjustAccepted = adjustPlan(planId, "换一家餐厅，要能订位的");
            assertEquals(202, adjustAccepted.statusCode(), adjustAccepted.body());
            HttpResponse<String> adjustStream = streamPlan(planId);
            assertEquals(200, adjustStream.statusCode(), adjustStream.body());
        }

        HttpResponse<String> fourthAdjust = adjustPlan(planId, "再换一家餐厅");
        assertEquals(429, fourthAdjust.statusCode());
        JsonNode json = objectMapper.readTree(fourthAdjust.body());
        assertEquals("ADJUST_LIMIT_EXCEEDED", json.path("error").asText());
        assertEquals("已达最大微调次数，请直接确认或重新发起规划", json.path("message").asText());
    }

    private String createPlanId(String text, String scenario) throws Exception {
        HttpResponse<String> response = postPlan("""
                {
                  "text": "%s",
                  "scenario": "%s",
                  "origin": "当前位置"
                }
                """.formatted(text, scenario));
        assertEquals(202, response.statusCode());
        JsonNode json = objectMapper.readTree(response.body());
        String planId = json.path("planId").asText();
        assertFalse(planId.isBlank(), response.body());
        return planId;
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

    private HttpResponse<String> clarifyPlan(String planId, String reply) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/api/plan/" + planId + "/clarify"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {
                          "reply": "%s"
                        }
                        """.formatted(reply), StandardCharsets.UTF_8))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> adjustPlan(String planId, String instruction) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/api/plan/" + planId + "/adjust"))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString("""
                        {
                          "instruction": "%s"
                        }
                        """.formatted(instruction), StandardCharsets.UTF_8))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> executePlan(String planId, boolean confirmed) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/api/plan/" + planId + "/execute"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {
                          "confirmed": %s
                        }
                        """.formatted(confirmed), StandardCharsets.UTF_8))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> streamPlan(String planId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/api/plan/" + planId + "/stream"))
                .header("Accept", "text/event-stream")
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
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
