package com.weekendtravel.backend.plan;

import com.weekendtravel.backend.plan.api.CreatePlanRequest;
import com.weekendtravel.backend.plan.api.CreatePlanResponse;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class PlanCreateService {

    private static final Set<String> VALID_SCENARIOS = Set.of("family", "friends");
    private static final String STATUS_PROCESSING = "processing";

    private final PlanStateMachineService planStateMachineService;

    public PlanCreateService(PlanStateMachineService planStateMachineService) {
        this.planStateMachineService = planStateMachineService;
    }

    public CreatePlanResponse createPlan(CreatePlanRequest request) {
        CreatePlanRequest normalized = normalize(request);
        String planId = generatePlanId();
        planStateMachineService.createPlan(planId, normalized);
        return new CreatePlanResponse(planId, STATUS_PROCESSING);
    }

    private CreatePlanRequest normalize(CreatePlanRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        String text = requireText(request.text(), "text");
        String scenario = requireScenario(request.scenario());
        String origin = optionalText(request.origin());
        return new CreatePlanRequest(text, scenario, origin);
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private String requireScenario(String scenario) {
        String normalized = requireText(scenario, "scenario").toLowerCase(Locale.ROOT);
        if (!VALID_SCENARIOS.contains(normalized)) {
            throw new IllegalArgumentException("scenario must be family or friends");
        }
        return normalized;
    }

    private String optionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String generatePlanId() {
        return "plan_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
