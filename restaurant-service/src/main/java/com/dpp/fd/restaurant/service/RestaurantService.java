package com.dpp.fd.restaurant.service;

import com.dpp.fd.restaurant.document.MenuItem;
import com.dpp.fd.restaurant.document.Restaurant;
import com.dpp.fd.restaurant.dto.*;
import com.dpp.fd.restaurant.exception.ForbiddenException;
import com.dpp.fd.restaurant.exception.ResourceNotFoundException;
import com.dpp.fd.restaurant.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Service
@RequiredArgsConstructor
public class RestaurantService {

    private final RestaurantRepository repository;

    public List<RestaurantDto> getAllOpen(String city, String cuisine) {
        log.debug("Listing open restaurants — city={} cuisine={}", city, cuisine);
        List<Restaurant> results;
        if (city != null)         results = repository.findByCityAndIsOpenTrue(city);
        else if (cuisine != null) results = repository.findByCuisineIgnoreCaseAndIsOpenTrue(cuisine);
        else                      results = repository.findAll();
        log.debug("Found {} restaurant(s)", results.size());
        return results.stream().map(this::toDto).toList();
    }

    @Cacheable(value = "restaurants", key = "#id")
    public RestaurantDto getById(String id) {
        log.debug("Fetching restaurant id={}", id);
        RestaurantDto res = toDto(findOrThrow(id));
        log.info("RestaurantSvc getById restaurant id={}  isOpen:{}", id,res.isOpen());
        return res;
    }

    public RestaurantDto create(String ownerId, CreateRestaurantRequest req) {
        log.info("Creating restaurant name='{}' cuisine='{}' city='{}' ownerId={}",
                req.getName(), req.getCuisine(), req.getCity(), ownerId);
        Restaurant restaurant = Restaurant.builder()
                .ownerId(ownerId)
                .name(req.getName())
                .cuisine(req.getCuisine())
                .city(req.getCity())
                .isOpen(true)
                .build();
        Restaurant saved = repository.save(restaurant);
        log.info("Restaurant created id={}", saved.getId());
        return toDto(saved);
    }

    @CacheEvict(value = "restaurants", key = "#id")
    public RestaurantDto updateMenu(String id, String ownerId, UpdateMenuRequest req) {
        Restaurant restaurant = findOrThrow(id);
        verifyOwnership(restaurant, ownerId);
        log.info("Updating menu for restaurantId={} — {} item(s)", id, req.getItems().size());
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
        Restaurant saved = repository.save(restaurant);
        log.info("Restaurant {} '{}' is now {}", id, saved.getName(), saved.isOpen() ? "OPEN" : "CLOSED");
        return toDto(saved);
    }

    // --- helpers ---

    private Restaurant findOrThrow(String id) {
    		Restaurant res= repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found: " + id));
    		log.info("findOrThrow()  restaurantId={} isOpen:{}",
    				id, res.isOpen());
    		return res;
    }

    private void verifyOwnership(Restaurant restaurant, String ownerId) {
        if (!restaurant.getOwnerId().equals(ownerId)) {
            log.warn("Ownership check failed restaurantId={} claimedOwner={} actualOwner={}",
                    restaurant.getId(), ownerId, restaurant.getOwnerId());
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
