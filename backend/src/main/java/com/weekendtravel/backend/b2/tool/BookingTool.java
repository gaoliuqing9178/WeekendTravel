package com.weekendtravel.backend.b2.tool;

import com.weekendtravel.backend.b2.model.Poi;
import com.weekendtravel.backend.b2.repository.PoiRepository;
import com.weekendtravel.backend.b2.scenario.ScenarioFlags;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BookingTool {

    private static final String EVENT_TYPE = "execute_result";
    private static final String STATUS_SUCCESS = "success";
    private static final String STATUS_FAILED = "failed";
    private static final String ACTION_CANCEL_BOOKING = "cancel_booking";
    private static final Set<String> SUPPORTED_ACTION_TYPES = Set.of(
            "buy_ticket",
            "reserve_table",
            "take_number",
            "schedule_delivery",
            "add_note",
            ACTION_CANCEL_BOOKING
    );
    private static final Map<String, String> CONFIRMATION_PREFIXES = Map.of(
            "buy_ticket", "MOCK-TKT-",
            "reserve_table", "MOCK-TBL-",
            "take_number", "MOCK-QNO-",
            "schedule_delivery", "MOCK-DLV-",
            "add_note", "MOCK-NOTE-",
            ACTION_CANCEL_BOOKING, "MOCK-CXL-"
    );

    private final PoiRepository poiRepository;
    private final ScenarioFlags scenarioFlags;
    private final Map<String, BookingResult> idempotencyCache = new ConcurrentHashMap<>();

    public BookingTool(PoiRepository poiRepository) {
        this(poiRepository, new ScenarioFlags());
    }

    @Autowired
    public BookingTool(PoiRepository poiRepository, ScenarioFlags scenarioFlags) {
        this.poiRepository = poiRepository;
        this.scenarioFlags = scenarioFlags;
    }

    public BookingResult execute(BookingRequest request) {
        return bookOrOrder(request);
    }

    public BookingResult bookOrOrder(BookingRequest request) {
        long start = System.currentTimeMillis();
        BookingRequest normalized = normalize(request);
        validateTarget(normalized);
        return idempotencyCache.computeIfAbsent(
                normalized.idempotencyKey(),
                ignored -> createResult(normalized, start)
        );
    }

    private BookingRequest normalize(BookingRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("booking request is required");
        }
        String planId = requireText(request.planId(), "planId");
        String actionId = requireText(request.actionId(), "actionId");
        String actionType = requireText(request.actionType(), "actionType").toLowerCase(Locale.ROOT);
        if (!SUPPORTED_ACTION_TYPES.contains(actionType)) {
            throw new IllegalArgumentException("unsupported actionType: " + actionType);
        }
        String idempotencyKey = requireText(request.idempotencyKey(), "idempotencyKey");
        String targetPoiId = optionalText(request.targetPoiId());
        if (!ACTION_CANCEL_BOOKING.equals(actionType) && targetPoiId == null) {
            throw new IllegalArgumentException("targetPoiId is required");
        }

        return new BookingRequest(
                planId,
                actionId,
                actionType,
                targetPoiId,
                optionalText(request.description()),
                idempotencyKey,
                optionalText(request.previousConfirmationNo())
        );
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private String optionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private void validateTarget(BookingRequest request) {
        if (ACTION_CANCEL_BOOKING.equals(request.actionType())) {
            return;
        }

        Poi poi = poiRepository.findById(request.targetPoiId())
                .orElseThrow(() -> new IllegalArgumentException("unknown targetPoiId: " + request.targetPoiId()));
        if (!poi.actionTypes().contains(request.actionType())) {
            throw new IllegalArgumentException(
                    "actionType " + request.actionType() + " is not supported by poi: " + request.targetPoiId()
            );
        }
    }

    private BookingResult createResult(BookingRequest request, long start) {
        long timestamp = System.currentTimeMillis();
        if (scenarioFlags.current().bookingFail() && !ACTION_CANCEL_BOOKING.equals(request.actionType())) {
            return new BookingResult(
                    EVENT_TYPE,
                    request.planId(),
                    request.actionId(),
                    request.actionType(),
                    STATUS_FAILED,
                    null,
                    "bookingFail scenario triggered for " + request.actionType(),
                    timestamp,
                    timestamp - start
            );
        }

        String confirmationNo = confirmationNo(request);
        return new BookingResult(
                EVENT_TYPE,
                request.planId(),
                request.actionId(),
                request.actionType(),
                STATUS_SUCCESS,
                confirmationNo,
                successMessage(request, confirmationNo),
                timestamp,
                timestamp - start
        );
    }

    private String confirmationNo(BookingRequest request) {
        String prefix = CONFIRMATION_PREFIXES.get(request.actionType());
        int suffix = Math.floorMod(request.idempotencyKey().hashCode(), 100000);
        return prefix + String.format(Locale.ROOT, "%05d", suffix);
    }

    private String successMessage(BookingRequest request, String confirmationNo) {
        if (ACTION_CANCEL_BOOKING.equals(request.actionType())) {
            String previous = request.previousConfirmationNo() == null
                    ? "previous booking"
                    : request.previousConfirmationNo();
            return "cancelled " + previous + " with " + confirmationNo;
        }
        if (request.description() == null) {
            return "mock action completed with " + confirmationNo;
        }
        return request.description() + " -> " + confirmationNo;
    }
}
