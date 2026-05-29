package com.weekendtravel.backend.api;

import com.weekendtravel.backend.plan.AdjustLimitExceededException;
import com.weekendtravel.backend.plan.InvalidPlanStateException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Pattern REQUIRED_FIELD_PATTERN = Pattern.compile("^([A-Za-z][A-Za-z0-9]*) is required$");

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse(
                "INVALID_INPUT",
                fallbackMessage(exception.getMessage()),
                extractDetails(exception.getMessage())
        ));
    }

    @ExceptionHandler(InvalidPlanStateException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidPlanState(InvalidPlanStateException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiErrorResponse(
                "INVALID_STATE",
                fallbackMessage(exception.getMessage()),
                null
        ));
    }

    @ExceptionHandler(AdjustLimitExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleAdjustLimitExceeded(AdjustLimitExceededException exception) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(new ApiErrorResponse(
                "ADJUST_LIMIT_EXCEEDED",
                fallbackMessage(exception.getMessage()),
                null
        ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableMessage(HttpMessageNotReadableException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiErrorResponse(
                "INVALID_INPUT",
                "request body is required",
                null
        ));
    }

    private String fallbackMessage(String message) {
        return message == null || message.isBlank() ? "invalid request" : message;
    }

    private Map<String, Object> extractDetails(String message) {
        if (message == null) {
            return null;
        }
        Matcher matcher = REQUIRED_FIELD_PATTERN.matcher(message);
        if (!matcher.matches()) {
            return null;
        }
        return Map.of("field", matcher.group(1));
    }
}
