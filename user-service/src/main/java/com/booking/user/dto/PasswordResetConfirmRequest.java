package com.booking.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmRequest(

        @NotBlank
        String token,

        @NotBlank @Size(min = 8, max = 100, message = "Lozinka mora imati najmanje 8 karaktera")
        String newPassword
) {
}
