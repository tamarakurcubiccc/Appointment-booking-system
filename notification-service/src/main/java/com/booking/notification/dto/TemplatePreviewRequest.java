package com.booking.notification.dto;

import java.util.Map;

public record TemplatePreviewRequest(Map<String, String> parameters) {
}
