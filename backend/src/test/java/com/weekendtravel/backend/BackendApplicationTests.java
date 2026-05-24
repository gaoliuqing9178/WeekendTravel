package com.weekendtravel.backend;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackendApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void healthReturnsContractFields() throws Exception {
        Object controller = Class.forName("com.weekendtravel.backend.controller.HealthController")
                .getDeclaredConstructor()
                .newInstance();

        Object response = controller.getClass().getMethod("health").invoke(controller);
        Method status = response.getClass().getMethod("status");
        Method service = response.getClass().getMethod("service");
        Method timestamp = response.getClass().getMethod("timestamp");

        assertEquals("ok", status.invoke(response));
        assertEquals("WeekendTravel", service.invoke(response));
        assertTrue((Long) timestamp.invoke(response) > 0);
    }
}
