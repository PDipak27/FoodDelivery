package com.dpp.fd.restaurant.controller;

import com.dpp.fd.restaurant.dto.*;
import com.dpp.fd.restaurant.exception.ForbiddenException;
import com.dpp.fd.restaurant.service.RestaurantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService restaurantService;

    @GetMapping
    public List<RestaurantDto> getAll(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String cuisine) {
        return restaurantService.getAllOpen(city, cuisine);
    }

    @GetMapping("/{id}")
    public RestaurantDto getById(@PathVariable String id) {
        return restaurantService.getById(id);
    }

    @PostMapping
    public ResponseEntity<RestaurantDto> create(
            @RequestHeader("X-User-Id") String ownerId,
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody CreateRestaurantRequest request) {
        requireRole(role, "RESTAURANT_OWNER");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(restaurantService.create(ownerId, request));
    }

    @PutMapping("/{id}/menu")
    public RestaurantDto updateMenu(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String ownerId,
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody UpdateMenuRequest request) {
        requireRole(role, "RESTAURANT_OWNER");
        return restaurantService.updateMenu(id, ownerId, request);
    }

    @PatchMapping("/{id}/toggle")
    public RestaurantDto toggleOpen(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String ownerId,
            @RequestHeader("X-User-Role") String role) {
        requireRole(role, "RESTAURANT_OWNER");
        return restaurantService.toggleOpen(id, ownerId);
    }

    private void requireRole(String actual, String expected) {
        if (!expected.equals(actual)) {
            throw new ForbiddenException("Role " + expected + " required");
        }
    }
}
