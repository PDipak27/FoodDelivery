package com.dpp.fd.restaurant.repository;

import com.dpp.fd.restaurant.document.Restaurant;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface RestaurantRepository extends MongoRepository<Restaurant, String> {
    List<Restaurant> findByCityAndIsOpenTrue(String city);
    List<Restaurant> findByCuisineIgnoreCaseAndIsOpenTrue(String cuisine);
    List<Restaurant> findByOwnerId(String ownerId);
}
