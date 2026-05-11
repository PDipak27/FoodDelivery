package com.dpp.fd.order.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/**
 * Notification calls are best-effort — a failed email must not roll back an order.
 * Exceptions are caught and logged; never re-thrown to the caller.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationClient {

    private final RestClient notificationRestClient;

    public void send(String to, String templateName, Map<String, String> vars) {
        try {
            notificationRestClient.post()
                    .uri("/notifications/send")
                    .body(new SendNotificationRequest(to, templateName, vars))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            log.warn("Notification send failed (non-critical): template={}, error={}", templateName, ex.getMessage());
        }
    }

    public record SendNotificationRequest(String to, String templateName, Map<String, String> vars) {}
}
