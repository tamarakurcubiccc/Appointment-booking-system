package com.booking.catalog.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record GenerateSlotsRequest(
        @NotNull LocalDate fromDate,
        @NotNull LocalDate toDate,
        @NotNull LocalTime workDayStart,
        @NotNull LocalTime workDayEnd,
        boolean skipWeekend,
        String zoneId
) {
}
