package com.weekendtravel.backend.controller;

import com.weekendtravel.backend.plan.PlanCreateService;
import com.weekendtravel.backend.plan.PlanStateMachineService;
import com.weekendtravel.backend.plan.PlanStreamService;
import com.weekendtravel.backend.plan.api.AdjustPlanRequest;
import com.weekendtravel.backend.plan.api.AdjustPlanResponse;
import com.weekendtravel.backend.plan.api.ClarifyPlanRequest;
import com.weekendtravel.backend.plan.api.ClarifyPlanResponse;
import com.weekendtravel.backend.plan.api.CreatePlanRequest;
import com.weekendtravel.backend.plan.api.CreatePlanResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/plan")
public class PlanController {

    private final PlanCreateService planCreateService;
    private final PlanStreamService planStreamService;
    private final PlanStateMachineService planStateMachineService;

    public PlanController(
            PlanCreateService planCreateService,
            PlanStreamService planStreamService,
            PlanStateMachineService planStateMachineService
    ) {
        this.planCreateService = planCreateService;
        this.planStreamService = planStreamService;
        this.planStateMachineService = planStateMachineService;
    }

    @PostMapping
    public ResponseEntity<CreatePlanResponse> create(@RequestBody CreatePlanRequest request) {
        return ResponseEntity.accepted().body(planCreateService.createPlan(request));
    }

    @GetMapping(path = "/{planId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable String planId, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        return planStreamService.openStream(planId);
    }

    @PostMapping("/{planId}/clarify")
    public ResponseEntity<ClarifyPlanResponse> clarify(@PathVariable String planId, @RequestBody ClarifyPlanRequest request) {
        return ResponseEntity.ok(planStateMachineService.clarifyPlan(planId, request));
    }

    @PatchMapping("/{planId}/adjust")
    public ResponseEntity<AdjustPlanResponse> adjust(@PathVariable String planId, @RequestBody AdjustPlanRequest request) {
        return ResponseEntity.accepted().body(planStateMachineService.adjustPlan(planId, request));
    }
}
