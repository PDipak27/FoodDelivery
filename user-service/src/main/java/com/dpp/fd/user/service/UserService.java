package com.dpp.fd.user.service;

import com.dpp.fd.user.dto.UpdateProfileRequest;
import com.dpp.fd.user.dto.UserProfileDto;
import com.dpp.fd.user.entity.UserProfile;
import com.dpp.fd.user.exception.ResourceNotFoundException;
import com.dpp.fd.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Manages customer profile data. The userId is the same UUID issued
 * by auth-service — no cross-service FK, linked only by shared ID convention.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserProfileRepository repository;

    public UserProfileDto getProfile(UUID userId) {
        return toDto(repository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user: " + userId)));
    }

    @Transactional
    public UserProfileDto createOrUpdateProfile(UUID userId, UpdateProfileRequest request) {
        UserProfile profile = repository.findById(userId).orElse(UserProfile.builder().id(userId).build());
        profile.setName(request.getName());
        profile.setPhone(request.getPhone());
        profile.setAddressLine(request.getAddressLine());
        profile.setCity(request.getCity());
        return toDto(repository.save(profile));
    }

    private UserProfileDto toDto(UserProfile p) {
        return UserProfileDto.builder()
                .id(p.getId()).name(p.getName())
                .phone(p.getPhone()).addressLine(p.getAddressLine()).city(p.getCity())
                .build();
    }
}
