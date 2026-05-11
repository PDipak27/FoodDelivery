package com.dpp.fd.restaurant.service;

import com.dpp.fd.restaurant.document.MenuItem;
import com.dpp.fd.restaurant.document.Restaurant;
import com.dpp.fd.restaurant.dto.*;
import com.dpp.fd.restaurant.exception.ForbiddenException;
import com.dpp.fd.restaurant.exception.ResourceNotFoundException;
import com.dpp.fd.restaurant.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Handles restaurant CRUD and menu management.
 * Read-heavy getById results are cached in Redis ("restaurants" cache, TTL 5 min).
 * Cache is evicted on any mutation to keep reads consistent.
 */
@Service
@RequiredArgsConstructor
public class RestaurantService {

    private final RestaurantRepository repository;

    public List<RestaurantDto> getAllOpen(String city, String cuisine) {
        List<Restaurant> results;
        if (city != null)        results = repository.findByCityAndIsOpenTrue(city);
        else if (cuisine != null) results = repository.findByCuisineIgnoreCaseAndIsOpenTrue(cuisine);
        else                      results = repository.findAll();
        return results.stream().map(this::toDto).toList();
    }

    @Cacheable(value = "restaurants", key = "#id")
    public RestaurantDto getById(String id) {
        return toDto(findOrThrow(id));
    }

    public RestaurantDto create(String ownerId, CreateRestaurantRequest req) {
        Restaurant restaurant = Restaurant.builder()
                .ownerId(ownerId)
                .name(req.getName())
                .cuisine(req.getCuisine())
                .city(req.getCity())
                .isOpen(true)
                .build();
        return toDto(repository.save(restaurant));
    }

    @CacheEvict(value = "restaurants", key = "#id")
    public RestaurantDto updateMenu(String id, String ownerId, UpdateMenuRequest req) {
        Restaurant restaurant = findOrThrow(id);
        verifyOwnership(restaurant, ownerId);
        List<MenuItem> items = req.getItems().stream()
                .map(dto -> MenuItem.builder()
                        .itemId(dto.getItemId() != null ? dto.getItemId() : UUID.randomUUID().toString())
                        .name(dto.getName())
                        .price(dto.getPrice())
                        .available(dto.isAvailable())
                        .build())
                .toList();
        restaurant.setMenu(items);
        return toDto(repository.save(restaurant));
    }

    @CacheEvict(value = "restaurants", key = "#id")
    public RestaurantDto toggleOpen(String id, String ownerId) {
        Restaurant restaurant = findOrThrow(id);
        verifyOwnership(restaurant, ownerId);
        restaurant.setOpen(!restaurant.isOpen());
        return toDto(repository.save(restaurant));
    }

    // --- helpers ---

    private Restaurant findOrThrow(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found: " + id));
    }

    private void verifyOwnership(Restaurant restaurant, String ownerId) {
        if (!restaurant.getOwnerId().equals(ownerId)) {
            throw new ForbiddenException("You do not own this restaurant");
        }
    }

    private RestaurantDto toDto(Restaurant r) {
        List<MenuItemDto> menuDtos = r.getMenu().stream()
                .map(item -> MenuItemDto.builder()
                        .itemId(item.getItemId())
                        .name(item.getName())
                        .price(item.getPrice())
                        .available(item.isAvailable())
                        .build())
                .toList();
        return RestaurantDto.builder()
                .id(r.getId()).name(r.getName()).cuisine(r.getCuisine())
                .city(r.getCity()).isOpen(r.isOpen()).menu(menuDtos)
                .build();
    }
}
