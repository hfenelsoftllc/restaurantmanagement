package com.hfenelsoftllc.foodcatalogue.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiErrorResponse(
        @Schema(description = "Error timestamp", example = "2026-06-01T10:15:30Z")
        Instant timestamp,
        @Schema(description = "HTTP status code", example = "404")
        int status,
        @Schema(description = "HTTP error reason", example = "Not Found")
        String error,
        @Schema(description = "Human-readable error message", example = "Food item not found for id: f-101")
        String message,
        @Schema(description = "Request path that caused the error", example = "/food-items/f-101")
        String path,
        @Schema(description = "Validation or error details")
        Map<String, String> details
) {
    public static ApiErrorResponse of(HttpStatus status, String message, String path) {
        return new ApiErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message, path, Map.of());
    }

    public static ApiErrorResponse of(HttpStatus status, String message, String path, Map<String, String> details) {
        return new ApiErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message, path, details);
    }
}

