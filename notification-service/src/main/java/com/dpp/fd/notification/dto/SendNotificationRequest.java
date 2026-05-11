package com.dpp.fd.notification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
public class SendNotificationRequest {
    @Email @NotBlank  private String to;
    @NotBlank         private String templateName;
    private Map<String, String> vars;
}
