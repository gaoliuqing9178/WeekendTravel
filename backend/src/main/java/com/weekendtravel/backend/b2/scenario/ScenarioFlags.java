package com.weekendtravel.backend.b2.scenario;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
public class ScenarioFlags {

    private final AtomicReference<ScenarioFlagsState> state = new AtomicReference<>(ScenarioFlagsState.disabled());

    public ScenarioFlagsState current() {
        return state.get();
    }

    public ScenarioFlagsState update(ScenarioFlagsUpdateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("scenario flags request is required");
        }

        return state.updateAndGet(current -> new ScenarioFlagsState(
                valueOrCurrent(request.restaurantFull(), current.restaurantFull()),
                valueOrCurrent(request.routeTooFar(), current.routeTooFar()),
                valueOrCurrent(request.bookingFail(), current.bookingFail()),
                valueOrCurrent(request.ageMismatch(), current.ageMismatch())
        ));
    }

    public ScenarioFlagsState replace(ScenarioFlagsState next) {
        if (next == null) {
            throw new IllegalArgumentException("scenario flags state is required");
        }
        state.set(next);
        return next;
    }

    public ScenarioFlagsState reset() {
        return replace(ScenarioFlagsState.disabled());
    }

    private boolean valueOrCurrent(Boolean requested, boolean current) {
        return requested == null ? current : requested;
    }
}
