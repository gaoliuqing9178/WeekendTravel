package com.weekendtravel.backend.b2;

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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DebugScenarioControllerTests {

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void postDebugScenarioUpdatesFlagsWithoutRestart() throws Exception {
        JsonNode firstUpdated = postFlags("""
                {
                  "restaurantFull": true,
                  "routeTooFar": true,
                  "bookingFail": false,
                  "ageMismatch": true
                }
                """);

        assertEquals(true, firstUpdated.path("restaurantFull").asBoolean());
        assertEquals(true, firstUpdated.path("routeTooFar").asBoolean());
        assertEquals(false, firstUpdated.path("bookingFail").asBoolean());
        assertEquals(true, firstUpdated.path("ageMismatch").asBoolean());

        JsonNode secondUpdated = postFlags("""
                {
                  "restaurantFull": false,
                  "routeTooFar": false,
                  "bookingFail": true,
                  "ageMismatch": false
                }
                """);

        assertEquals(false, secondUpdated.path("restaurantFull").asBoolean());
        assertEquals(false, secondUpdated.path("routeTooFar").asBoolean());
        assertEquals(true, secondUpdated.path("bookingFail").asBoolean());
        assertEquals(false, secondUpdated.path("ageMismatch").asBoolean());
    }

    private JsonNode postFlags(String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/api/debug/scenario"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        assertEquals(200, response.statusCode());
        return objectMapper.readTree(response.body()).path("updated");
    }
}
