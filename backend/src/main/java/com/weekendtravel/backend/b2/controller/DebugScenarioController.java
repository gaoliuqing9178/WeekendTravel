package com.weekendtravel.backend.b2.controller;

import com.weekendtravel.backend.b2.scenario.ScenarioFlags;
import com.weekendtravel.backend.b2.scenario.ScenarioFlagsResponse;
import com.weekendtravel.backend.b2.scenario.ScenarioFlagsUpdateRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DebugScenarioController {

    private final ScenarioFlags scenarioFlags;

    public DebugScenarioController(ScenarioFlags scenarioFlags) {
        this.scenarioFlags = scenarioFlags;
    }

    @PostMapping("/api/debug/scenario")
    public ScenarioFlagsResponse update(@RequestBody ScenarioFlagsUpdateRequest request) {
        return new ScenarioFlagsResponse(scenarioFlags.update(request));
    }
}
