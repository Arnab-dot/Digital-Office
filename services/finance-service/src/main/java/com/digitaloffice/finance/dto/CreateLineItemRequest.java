package com.digitaloffice.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateLineItemRequest(
    @NotBlank(message = "description is required") String description,
    @Positive(message = "quantity must be positive") int quantity,
    @NotNull(message = "unitPrice is required") BigDecimal unitPrice
) {}

