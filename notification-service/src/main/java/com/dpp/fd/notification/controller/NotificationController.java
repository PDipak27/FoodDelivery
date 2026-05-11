package com.dpp.fd.notification.controller;

import com.dpp.fd.notification.dto.SendNotificationRequest;
import com.dpp.fd.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /** Internal endpoint — called only by other services, not exposed publicly via gateway. */
    @PostMapping("/send")
    public ResponseEntity<Void> send(@Valid @RequestBody SendNotificationRequest request) {
        notificationService.send(request);
        return ResponseEntity.accepted().build();
    }
}
