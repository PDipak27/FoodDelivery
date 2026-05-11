package com.dpp.fd.user.service;

import com.dpp.fd.user.dto.UpdateProfileRequest;
import com.dpp.fd.user.entity.UserProfile;
import com.dpp.fd.user.exception.ResourceNotFoundException;
import com.dpp.fd.user.repository.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserProfileRepository repository;
    @InjectMocks private UserService userService;

    @Test
    void getProfile_existing_returnsDto() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(
                UserProfile.builder().id(id).name("Alice").city("Mumbai").build()));

        var dto = userService.getProfile(id);

        assertThat(dto.getName()).isEqualTo("Alice");
        assertThat(dto.getId()).isEqualTo(id);
    }

    @Test
    void getProfile_notFound_throwsException() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createOrUpdateProfile_newUser_savesProfile() {
        UUID id = UUID.randomUUID();
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setName("Bob"); req.setCity("Delhi");

        when(repository.findById(id)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var dto = userService.createOrUpdateProfile(id, req);

        assertThat(dto.getName()).isEqualTo("Bob");
        verify(repository).save(any(UserProfile.class));
    }
}
