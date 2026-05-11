package com.dpp.fd.user.controller;

import com.dpp.fd.user.dto.UpdateProfileRequest;
import com.dpp.fd.user.dto.UserProfileDto;
import com.dpp.fd.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public UserProfileDto getProfile(@RequestHeader("X-User-Id") UUID userId) {
        return userService.getProfile(userId);
    }

    @PutMapping("/me")
    public UserProfileDto upsertProfile(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        return userService.createOrUpdateProfile(userId, request);
    }
}
