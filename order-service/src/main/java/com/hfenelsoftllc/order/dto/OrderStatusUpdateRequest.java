package com.hfenelsoftllc.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class OrderStatusUpdateRequest {

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "PAID|CANCELLED", message = "Status must be PAID or CANCELLED")
    private String status;
}

