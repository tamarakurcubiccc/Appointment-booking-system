package com.booking.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record NotificationTemplateRequest(

        @NotBlank @Size(max = 60) @Pattern(regexp = "[A-Z0-9_]+", message = "Dozvoljena su samo velika slova, cifre i _")
        String code,

        @Size(max = 200)
        String description,

        @NotBlank @Size(max = 200)
        String subject,

        @NotBlank @Size(max = 4000)
        String body
) {
}
