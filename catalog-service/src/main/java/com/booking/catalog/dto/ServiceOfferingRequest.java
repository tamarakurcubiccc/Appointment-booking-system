package com.booking.catalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ServiceOfferingRequest(

        @NotBlank @Size(max = 140)
        String name,

        @Size(max = 1000)
        String description,

        @Min(5) @Max(600)
        int durationMinutes,

        @NotNull @DecimalMin("0.0")
        BigDecimal price
) {
}
