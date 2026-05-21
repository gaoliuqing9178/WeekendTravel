package com.weekendtravel.backend;

import com.weekendtravel.backend.controller.HealthController;
import com.weekendtravel.backend.dto.HealthResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackendApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void healthReturnsContractFields() {
        HealthController controller = new HealthController();

        HealthResponse response = controller.health();

        assertEquals("ok", response.status());
        assertEquals("WeekendTravel", response.service());
        assertTrue(response.timestamp() > 0);
    }
}
