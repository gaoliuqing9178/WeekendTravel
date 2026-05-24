package com.weekendtravel.backend.b2.scenario;

public record ScenarioFlagsUpdateRequest(
        Boolean restaurantFull,
        Boolean routeTooFar,
        Boolean bookingFail,
        Boolean ageMismatch
) {
}
