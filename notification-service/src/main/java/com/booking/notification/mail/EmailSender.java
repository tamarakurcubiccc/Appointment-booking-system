package com.booking.notification.mail;

import com.booking.notification.config.NotificationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class EmailSender {
    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

    private final JavaMailSender mailSender;
    private final NotificationProperties properties;

    public EmailSender(JavaMailSender mailSender, NotificationProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    public void send(String to, String subject, String body) {
        if (!properties.mailEnabled()) {
            log.info("Mail dispatch disabled - not sending \"{}\" to {}", subject, to);
            return;
        }

        var message = new SimpleMailMessage();
        message.setFrom(properties.fromAddress());
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
        log.info("Sent \"{}\" to {}", subject, to);
    }
}
