package com.dpp.fd.restaurant.service;

import com.dpp.fd.restaurant.document.Restaurant;
import com.dpp.fd.restaurant.dto.CreateRestaurantRequest;
import com.dpp.fd.restaurant.exception.ForbiddenException;
import com.dpp.fd.restaurant.exception.ResourceNotFoundException;
import com.dpp.fd.restaurant.repository.RestaurantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceTest {

    @Mock private RestaurantRepository repository;
    @InjectMocks private RestaurantService service;

    @Test
    void getById_existing_returnsDto() {
        Restaurant r = Restaurant.builder().id("r1").name("Pizza Place")
                .cuisine("Italian").city("Mumbai").isOpen(true).menu(List.of()).build();
        when(repository.findById("r1")).thenReturn(Optional.of(r));

        var dto = service.getById("r1");

        assertThat(dto.getId()).isEqualTo("r1");
        assertThat(dto.getName()).isEqualTo("Pizza Place");
    }

    @Test
    void getById_notFound_throwsException() {
        when(repository.findById("bad")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getById("bad"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_validRequest_savesAndReturns() {
        CreateRestaurantRequest req = new CreateRestaurantRequest();
        req.setName("Burger Barn"); req.setCuisine("American"); req.setCity("Delhi");

        when(repository.save(any())).thenAnswer(inv -> {
            Restaurant r = inv.getArgument(0);
            r.setId("new-id");
            return r;
        });

        var dto = service.create("owner-1", req);

        assertThat(dto.getName()).isEqualTo("Burger Barn");
        verify(repository).save(any(Restaurant.class));
    }

    @Test
    void toggleOpen_wrongOwner_throwsForbidden() {
        Restaurant r = Restaurant.builder().id("r1").ownerId("owner-A")
                .menu(List.of()).build();
        when(repository.findById("r1")).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> service.toggleOpen("r1", "owner-B"))
                .isInstanceOf(ForbiddenException.class);
    }
}
