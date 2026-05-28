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
class PlanFlowIntegrationTests {

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createThenStreamReachesPlanReady() throws Exception {
        resetScenarioFlags();
        HttpResponse<String> createResponse = postPlan("""
                {
                  "text": "今天下午想和老婆孩子找个轻松的亲子活动，再吃个不用排太久的晚饭",
                  "scenario": "family",
                  "origin": "当前位置"
                }
                """);

        assertEquals(202, createResponse.statusCode());
        JsonNode createJson = objectMapper.readTree(createResponse.body());
        String planId = createJson.path("planId").asText();
        assertFalse(planId.isBlank(), createResponse.body());

        HttpResponse<String> streamResponse = streamPlan(planId);
        assertEquals(200, streamResponse.statusCode());
        String bodyText = streamResponse.body();
        assertTrue(bodyText.contains("plan_ready"), bodyText);
        assertTrue(bodyText.contains(planId), bodyText);
        assertTrue(bodyText.contains("PACK"), bodyText);
        assertFalse(bodyText.contains("event:replan"), bodyText);
        assertTrue(bodyText.contains("\"isPlanB\":false"), bodyText);
        assertTrue(bodyText.contains("\"replanCount\":0"), bodyText);
    }

    @Test
    void friendsScenarioReachesPlanReadyWithFriendsCopy() throws Exception {
        resetScenarioFlags();
        HttpResponse<String> createResponse = postPlan("""
                {
                  "text": "今天下午4个人出去玩，想找个能拍照也能吃饭的地方",
                  "scenario": "friends",
                  "origin": "当前位置"
                }
                """);

        assertEquals(202, createResponse.statusCode());
        JsonNode createJson = objectMapper.readTree(createResponse.body());
        String planId = createJson.path("planId").asText();
        assertFalse(planId.isBlank(), createResponse.body());

        HttpResponse<String> streamResponse = streamPlan(planId);
        assertEquals(200, streamResponse.statusCode());
        String bodyText = streamResponse.body();
        assertTrue(bodyText.contains("plan_ready"), bodyText);
        assertTrue(bodyText.contains("\"scenario\":\"friends\""), bodyText);
        assertTrue(bodyText.contains("朋友活动搭配轻松聚餐"), bodyText);
        assertTrue(bodyText.contains("预约 18:20 的 4 人座位"), bodyText);
        assertFalse(bodyText.contains("适合 5 岁儿童"), bodyText);
    }

    @Test
    void restaurantFullScenarioTriggersReplanThenDegrade() throws Exception {
        updateScenarioFlags(true, false, false, false);
        try {
            String planId = createFamilyPlan();

            HttpResponse<String> streamResponse = streamPlan(planId);
            assertEquals(200, streamResponse.statusCode());
            String bodyText = streamResponse.body();
            assertTrue(bodyText.contains("event:replan"), bodyText);
            assertTrue(bodyText.contains("REPLAN"), bodyText);
            assertTrue(bodyText.contains("原餐厅排队预计"), bodyText);
            assertTrue(bodyText.contains("DEGRADE"), bodyText);
            assertTrue(bodyText.contains("event:error"), bodyText);
            assertTrue(bodyText.contains("\"code\":\"DEGRADE\""), bodyText);
            assertTrue(bodyText.contains("2 次重排后仍无可行方案"), bodyText);
            assertFalse(bodyText.contains("plan_ready"), bodyText);
        } finally {
            resetScenarioFlags();
        }
    }

    @Test
    void routeTooFarScenarioTriggersReplanThenDegrade() throws Exception {
        updateScenarioFlags(false, true, false, false);
        try {
            String planId = createFamilyPlan();

            HttpResponse<String> streamResponse = streamPlan(planId);
            assertEquals(200, streamResponse.statusCode());
            String bodyText = streamResponse.body();
            assertTrue(bodyText.contains("event:replan"), bodyText);
            assertTrue(bodyText.contains("REPLAN"), bodyText);
            assertTrue(bodyText.contains("原路线过远，已切换到更近餐厅"), bodyText);
            assertTrue(bodyText.contains("DEGRADE"), bodyText);
            assertTrue(bodyText.contains("event:error"), bodyText);
            assertTrue(bodyText.contains("\"code\":\"DEGRADE\""), bodyText);
            assertFalse(bodyText.contains("plan_ready"), bodyText);
        } finally {
            resetScenarioFlags();
        }
    }

    private String createFamilyPlan() throws Exception {
        HttpResponse<String> createResponse = postPlan("""
                {
                  "text": "今天下午想和老婆孩子找个轻松的亲子活动，再吃个不用排太久的晚饭",
                  "scenario": "family",
                  "origin": "当前位置"
                }
                """);
        assertEquals(202, createResponse.statusCode());
        JsonNode createJson = objectMapper.readTree(createResponse.body());
        String planId = createJson.path("planId").asText();
        assertFalse(planId.isBlank(), createResponse.body());
        return planId;
    }

    private void resetScenarioFlags() throws Exception {
        updateScenarioFlags(false, false, false, false);
    }

    private void updateScenarioFlags(boolean restaurantFull, boolean routeTooFar, boolean bookingFail, boolean ageMismatch) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/api/debug/scenario"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {
                          "restaurantFull": %s,
                          "routeTooFar": %s,
                          "bookingFail": %s,
                          "ageMismatch": %s
                        }
                        """.formatted(restaurantFull, routeTooFar, bookingFail, ageMismatch), StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, response.statusCode(), response.body());
    }

    private HttpResponse<String> postPlan(String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/api/plan"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> streamPlan(String planId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/api/plan/" + planId + "/stream"))
                .header("Accept", "text/event-stream")
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
}
