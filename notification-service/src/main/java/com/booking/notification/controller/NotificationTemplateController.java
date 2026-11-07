package com.booking.notification.controller;

import com.booking.notification.dto.NotificationTemplateRequest;
import com.booking.notification.dto.NotificationTemplateResponse;
import com.booking.notification.dto.NotificationTemplateUpdateRequest;
import com.booking.notification.dto.TemplatePreviewRequest;
import com.booking.notification.dto.TemplatePreviewResponse;
import com.booking.notification.service.NotificationTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notification-templates")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Šabloni obaveštenja")
public class NotificationTemplateController {
    private final NotificationTemplateService service;

    public NotificationTemplateController(NotificationTemplateService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Izlistaj sve šablone (ADMIN)")
    public List<NotificationTemplateResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{code}")
    @Operation(summary = "Preuzmi šablon po šifri (ADMIN)")
    public NotificationTemplateResponse findByCode(@PathVariable String code) {
        return service.findByCode(code);
    }

    @PostMapping
    @Operation(summary = "Kreiraj šablon (ADMIN)")
    public ResponseEntity<NotificationTemplateResponse> create(
            @Valid @RequestBody NotificationTemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{code}")
    @Operation(summary = "Izmeni tekst šablona ili ga deaktiviraj (ADMIN)")
    public NotificationTemplateResponse update(@PathVariable String code,
                                               @Valid @RequestBody NotificationTemplateUpdateRequest request) {
        return service.update(code, request);
    }

    @DeleteMapping("/{code}")
    @Operation(summary = "Obriši šablon (ADMIN)")
    public ResponseEntity<Void> delete(@PathVariable String code) {
        service.delete(code);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{code}/preview")
    @Operation(summary = "Probno renderuj šablon sa zadatim parametrima (ADMIN)")
    public TemplatePreviewResponse preview(@PathVariable String code,
                                           @RequestBody TemplatePreviewRequest request) {
        return service.preview(code, request.parameters());
    }
}
