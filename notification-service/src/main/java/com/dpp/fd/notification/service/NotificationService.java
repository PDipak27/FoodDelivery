package com.dpp.fd.notification.service;

import com.dpp.fd.notification.dto.SendNotificationRequest;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Renders Thymeleaf HTML email templates and sends via JavaMailSender.
 * Locally, JavaMailSender points at Mailpit (SMTP :1025) — emails are captured
 * in its web UI at :8025, no real delivery occurs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    public void send(SendNotificationRequest request) {
        try {
            Context ctx = new Context();
            if (request.getVars() != null) {
                request.getVars().forEach(ctx::setVariable);
            }

            String html = templateEngine.process(
                    "emails/" + request.getTemplateName().toLowerCase(), ctx);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(request.getTo());
            helper.setSubject(resolveSubject(request.getTemplateName()));
            helper.setText(html, true);

            mailSender.send(message);
            log.info("Email sent: template={} to={}", request.getTemplateName(), request.getTo());

        } catch (MessagingException ex) {
            log.error("Failed to send email to {}: {}", request.getTo(), ex.getMessage());
        }
    }

    private String resolveSubject(String templateName) {
        return switch (templateName.toUpperCase()) {
            case "ORDER_PLACED"    -> "Your order has been placed!";
            case "ORDER_ACCEPTED"  -> "Your order is accepted!";
            case "ORDER_REJECTED"  -> "Your order was rejected";
            case "ORDER_DELIVERED" -> "Your order has been delivered!";
            default                -> "Food Delivery Notification";
        };
    }
}
