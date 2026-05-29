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
        HttpResponse<String> createResponse = postPlan("今天下午想和老婆孩子找个轻松的亲子活动，再吃个不用排太久的晚饭", "family");

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
        assertTrue(bodyText.contains("CONFIRM"), bodyText);
        assertFalse(bodyText.contains("event:replan"), bodyText);
        assertTrue(bodyText.contains("\"isPlanB\":false"), bodyText);
        assertTrue(bodyText.contains("\"replanCount\":0"), bodyText);
    }

    @Test
    void ambiguousInputTriggersClarifyThenClarifyReplyResumesPlan() throws Exception {
        resetScenarioFlags();
        HttpResponse<String> createResponse = postPlan("今天下午是空的，想和老婆孩子出去玩几个小时", "family");
        assertEquals(202, createResponse.statusCode());
        String planId = objectMapper.readTree(createResponse.body()).path("planId").asText();
        assertFalse(planId.isBlank(), createResponse.body());

        HttpResponse<String> initialStream = streamPlan(planId);
        assertEquals(200, initialStream.statusCode());
        String initialBody = initialStream.body();
        assertTrue(initialBody.contains("CLARIFY"), initialBody);
        assertTrue(initialBody.contains("clarification_request"), initialBody);
        assertFalse(initialBody.contains("plan_ready"), initialBody);

        HttpResponse<String> clarifyResponse = clarifyPlan(planId, "4-6小时");
        assertEquals(200, clarifyResponse.statusCode());
        JsonNode clarifyJson = objectMapper.readTree(clarifyResponse.body());
        assertEquals("processing", clarifyJson.path("status").asText());

        HttpResponse<String> resumedStream = streamPlan(planId);
        assertEquals(200, resumedStream.statusCode());
        String resumedBody = resumedStream.body();
        assertTrue(resumedBody.contains("CLARIFY"), resumedBody);
        assertTrue(resumedBody.contains("INTENT"), resumedBody);
        assertTrue(resumedBody.contains("plan_ready"), resumedBody);
        assertTrue(resumedBody.contains("CONFIRM"), resumedBody);
    }

    @Test
    void adjustRequestEmitsAdjustResultAndKeepsPlanInConfirm() throws Exception {
        resetScenarioFlags();
        String planId = createFamilyPlanWithDuration();

        HttpResponse<String> initialStream = streamPlan(planId);
        assertEquals(200, initialStream.statusCode());
        String initialBody = initialStream.body();
        assertTrue(initialBody.contains("plan_ready"), initialBody);
        assertTrue(initialBody.contains("CONFIRM"), initialBody);

        HttpResponse<String> adjustResponse = adjustPlan(planId, "换一家餐厅，要能订位的");
        assertEquals(202, adjustResponse.statusCode());
        JsonNode adjustJson = objectMapper.readTree(adjustResponse.body());
        assertEquals("adjusting", adjustJson.path("status").asText());

        HttpResponse<String> adjustedStream = streamPlan(planId);
        assertEquals(200, adjustedStream.statusCode());
        String adjustedBody = adjustedStream.body();
        assertTrue(adjustedBody.contains("ADJUST"), adjustedBody);
        assertTrue(adjustedBody.contains("adjust_result"), adjustedBody);
        assertTrue(adjustedBody.contains("\"affectedSlots\":[\"restaurant\"]"), adjustedBody);
        assertTrue(adjustedBody.contains("CONFIRM"), adjustedBody);
    }

    @Test
    void friendsScenarioReachesPlanReadyWithFriendsCopy() throws Exception {
        resetScenarioFlags();
        HttpResponse<String> createResponse = postPlan("今天下午4个人出去玩，想找个能拍照也能吃饭的地方", "friends");

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
            String planId = createFamilyPlanWithDuration();

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
            String planId = createFamilyPlanWithDuration();

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

    private String createFamilyPlanWithDuration() throws Exception {
        HttpResponse<String> createResponse = postPlan("今天下午想和老婆孩子找个轻松的亲子活动，再吃个不用排太久的晚饭，安排4小时左右", "family");
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

    private HttpResponse<String> postPlan(String text, String scenario) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/api/plan"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {
                          "text": "%s",
                          "scenario": "%s",
                          "origin": "当前位置"
                        }
                        """.formatted(text, scenario), StandardCharsets.UTF_8))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
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
