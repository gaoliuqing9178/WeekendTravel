package com.weekendtravel.backend.b2.model;

import java.util.List;

public record Poi(
        String id,
        String name,
        String category,
        String subCategory,
        List<String> scenarios,
        String address,
        double lat,
        double lng,
        double rating,
        List<String> tags,
        int distanceMinutesFromCenter,
        int pricePerPerson,
        Range ageRequirement,
        Range groupSize,
        String availabilityStatus,
        int waitMinutes,
        DefaultAvailability defaultAvailability,
        PoiScenarioFlags scenarioFlags,
        List<String> actionTypes
) {
    public Poi {
        scenarios = scenarios == null ? List.of() : List.copyOf(scenarios);
        tags = tags == null ? List.of() : List.copyOf(tags);
        actionTypes = actionTypes == null ? List.of() : List.copyOf(actionTypes);
    }

    public boolean supportsScenario(String scenario) {
        return scenarios.contains(scenario)
                || ("family".equals(scenario) && scenarioFlags.family())
                || ("friends".equals(scenario) && scenarioFlags.friends());
    }

    public boolean supportsAge(Integer age) {
        return age == null || ageRequirement == null || ageRequirement.includes(age);
    }

    public boolean supportsGroupSize(Integer peopleCount) {
        return peopleCount == null || groupSize == null || groupSize.includes(peopleCount);
    }
}
