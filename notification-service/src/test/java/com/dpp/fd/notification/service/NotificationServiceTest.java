package com.dpp.fd.notification.service;

import com.dpp.fd.notification.dto.SendNotificationRequest;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private JavaMailSender mailSender;
    @Mock private TemplateEngine templateEngine;
    @Mock private MimeMessage mimeMessage;

    @InjectMocks private NotificationService notificationService;

    @Test
    void send_validRequest_invokesMailSender() throws Exception {
        SendNotificationRequest req = new SendNotificationRequest();
        req.setTo("user@test.com");
        req.setTemplateName("ORDER_PLACED");
        req.setVars(Map.of("orderId", "order-123", "total", "250.00"));

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(mimeMessage.getAllRecipients()).thenReturn(null);
        when(templateEngine.process(eq("emails/order_placed"), any(Context.class)))
                .thenReturn("<html>Order placed</html>");

        notificationService.send(req);

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void send_templateEngineThrows_doesNotPropagateException() {
        SendNotificationRequest req = new SendNotificationRequest();
        req.setTo("user@test.com");
        req.setTemplateName("ORDER_PLACED");

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(any(String.class), any(Context.class)))
                .thenReturn("<html></html>");

        // Should not throw even if mail send fails — notification is best-effort
        doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(MimeMessage.class));

        notificationService.send(req); // must not throw
    }
}
