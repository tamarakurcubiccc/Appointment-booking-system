package com.booking.user.dto;

import com.booking.user.entity.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequest(@NotNull Role role) {
}
