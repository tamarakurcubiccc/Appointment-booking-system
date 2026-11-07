package com.booking.notification.repository;

import com.booking.notification.entity.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {
    Optional<NotificationTemplate> findByCode(String code);

    boolean existsByCode(String code);

    List<NotificationTemplate> findAllByOrderByCodeAsc();
}
