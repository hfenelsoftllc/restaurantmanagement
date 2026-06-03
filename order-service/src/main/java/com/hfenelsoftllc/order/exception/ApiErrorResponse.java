package com.hfenelsoftllc.order.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiErrorResponse(
        @Schema(description = "Error timestamp") Instant timestamp,
        @Schema(description = "HTTP status code") int status,
        @Schema(description = "HTTP error reason") String error,
        @Schema(description = "Human-readable error message") String message,
        @Schema(description = "Request path") String path,
        @Schema(description = "Validation details") Map<String, String> details
) {
    public static ApiErrorResponse of(HttpStatus status, String message, String path) {
        return new ApiErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message, path, Map.of());
    }

    public static ApiErrorResponse of(HttpStatus status, String message, String path, Map<String, String> details) {
        return new ApiErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message, path, details);
    }
}

