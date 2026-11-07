package com.booking.notification.controller;

import com.booking.common.security.CurrentUser;
import com.booking.notification.dto.NotificationResponse;
import com.booking.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Obaveštenja")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping("/my")
    @Operation(summary = "Obaveštenja trenutnog korisnika")
    public Page<NotificationResponse> my(@RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size) {
        return service.findMine(CurrentUser.requiredId(), PageRequest.of(page, size));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Sva obaveštenja u sistemu (ADMIN)")
    public Page<NotificationResponse> findAll(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return service.findAll(PageRequest.of(page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Preuzmi jedno obaveštenje (primalac ili ADMIN)")
    public NotificationResponse findById(@PathVariable UUID id) {
        return service.findById(id, CurrentUser.requiredId(), CurrentUser.hasRole("ADMIN"));
    }
}
