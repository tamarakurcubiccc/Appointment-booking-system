package com.booking.catalog.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Generiše termine za svaki dan u opsegu [fromDate, toDate], između workDayStart i workDayEnd,
 * korakom jednakim trajanju usluge. Dani vikenda se preskaču kada je skipWeekend true.
 */
public record GenerateSlotsRequest(
        @NotNull LocalDate fromDate,
        @NotNull LocalDate toDate,
        @NotNull LocalTime workDayStart,
        @NotNull LocalTime workDayEnd,
        boolean skipWeekend,
        String zoneId
) {
}
