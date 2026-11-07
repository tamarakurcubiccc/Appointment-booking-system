package com.booking.notification.service;

import com.booking.common.exception.ApiException;
import com.booking.notification.dto.NotificationTemplateRequest;
import com.booking.notification.dto.NotificationTemplateResponse;
import com.booking.notification.dto.NotificationTemplateUpdateRequest;
import com.booking.notification.dto.TemplatePreviewResponse;
import com.booking.notification.entity.NotificationTemplate;
import com.booking.notification.repository.NotificationTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class NotificationTemplateService {
    private static final Logger log = LoggerFactory.getLogger(NotificationTemplateService.class);

    private final NotificationTemplateRepository repository;
    private final TemplateRenderer renderer;

    public NotificationTemplateService(NotificationTemplateRepository repository, TemplateRenderer renderer) {
        this.repository = repository;
        this.renderer = renderer;
    }

    @Transactional
    public NotificationTemplateResponse create(NotificationTemplateRequest request) {
        if (repository.existsByCode(request.code())) {
            throw ApiException.conflict("Šablon sa šifrom " + request.code() + " već postoji");
        }

        var template = new NotificationTemplate(request.code(), request.description(),
                request.subject(), request.body());
        template.setParameters(extractParameters(template));

        repository.save(template);
        log.info("Created notification template {}", template.getCode());
        return NotificationTemplateResponse.from(template);
    }

    @Transactional
    public NotificationTemplateResponse update(String code, NotificationTemplateUpdateRequest request) {
        var template = getOrThrow(code);

        template.setDescription(request.description());
        template.setSubject(request.subject());
        template.setBody(request.body());
        template.setActive(request.active());
        template.setParameters(extractParameters(template));
        template.markUpdated();

        log.info("Updated notification template {}", code);
        return NotificationTemplateResponse.from(template);
    }

    @Transactional
    public void delete(String code) {
        repository.delete(getOrThrow(code));
        log.info("Deleted notification template {}", code);
    }

    @Transactional(readOnly = true)
    public List<NotificationTemplateResponse> findAll() {
        return repository.findAllByOrderByCodeAsc().stream().map(NotificationTemplateResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public NotificationTemplateResponse findByCode(String code) {
        return NotificationTemplateResponse.from(getOrThrow(code));
    }

    @Transactional(readOnly = true)
    public Optional<NotificationTemplate> findActive(String code) {
        return repository.findByCode(code).filter(NotificationTemplate::isActive);
    }

    @Transactional(readOnly = true)
    public TemplatePreviewResponse preview(String code, Map<String, String> parameters) {
        var template = getOrThrow(code);
        var values = parameters == null ? Map.<String, String>of() : parameters;

        return new TemplatePreviewResponse(code,
                renderer.render(template.getSubject(), values),
                renderer.render(template.getBody(), values));
    }

    @Transactional
    public void createIfAbsent(String code, String description, String subject, String body) {
        if (repository.existsByCode(code)) {
            return;
        }

        var template = new NotificationTemplate(code, description, subject, body);
        template.setParameters(extractParameters(template));
        repository.save(template);
        log.info("Seeded default notification template {}", code);
    }

    private NotificationTemplate getOrThrow(String code) {
        return repository.findByCode(code)
                .orElseThrow(() -> ApiException.notFound("Šablon " + code + " nije pronađen"));
    }

    private String extractParameters(NotificationTemplate template) {
        var names = renderer.placeholders(template.getSubject());
        names.addAll(renderer.placeholders(template.getBody()));
        return String.join(",", names);
    }
}
