package com.booking.notification.dto;

import java.util.Map;

/** Probno renderovanje šablona - administrator vidi tekst pre nego što ga korisnici dobiju. */
public record TemplatePreviewRequest(Map<String, String> parameters) {
}
