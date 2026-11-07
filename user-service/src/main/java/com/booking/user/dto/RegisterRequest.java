package com.booking.user.dto;

import com.booking.user.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank @Email @Size(max = 180)
        String email,

        @NotBlank @Size(min = 8, max = 100, message = "Lozinka mora imati najmanje 8 karaktera")
        String password,

        @NotBlank @Size(max = 120)
        String fullName,

        @Size(max = 30)
        String phone,

        Role role
) {
}
