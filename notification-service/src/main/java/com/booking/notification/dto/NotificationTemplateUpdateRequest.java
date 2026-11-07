package com.booking.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Šifra šablona se ne menja - na nju se poziva kod koji šalje obaveštenja. */
public record NotificationTemplateUpdateRequest(

        @Size(max = 200)
        String description,

        @NotBlank @Size(max = 200)
        String subject,

        @NotBlank @Size(max = 4000)
        String body,

        boolean active
) {
}
