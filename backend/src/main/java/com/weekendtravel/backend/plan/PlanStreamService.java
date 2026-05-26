package com.weekendtravel.backend.plan;

import com.weekendtravel.backend.plan.sse.HeartbeatEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Service
public class PlanStreamService {

    private static final long STREAM_TIMEOUT_MILLIS = 15_000L;

    private final PlanStateMachineService planStateMachineService;

    public PlanStreamService(PlanStateMachineService planStateMachineService) {
        this.planStateMachineService = planStateMachineService;
    }

    public SseEmitter openStream(String planId) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MILLIS);

        try {
            sendHeartbeat(emitter, planId);
            planStateMachineService.streamPlan(planId, emitter);
            emitter.complete();
        } catch (IOException exception) {
            emitter.completeWithError(exception);
        }

        return emitter;
    }

    private void sendHeartbeat(SseEmitter emitter, String planId) throws IOException {
        HeartbeatEvent event = new HeartbeatEvent("heartbeat", planId, System.currentTimeMillis());
        emitter.send(SseEmitter.event().name(event.type()).data(event));
    }
}
