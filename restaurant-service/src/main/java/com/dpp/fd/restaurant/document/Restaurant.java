package com.dpp.fd.restaurant.document;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Top-level MongoDB document. Menu items are embedded to avoid a
 * separate collection — a restaurant and its menu are always read together.
 */
@Document(collection = "restaurants")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Restaurant {

    @Id
    private String id;

    private String ownerId;
    private String name;
    private String cuisine;
    private String city;
    private boolean isOpen;

    @Builder.Default
    private List<MenuItem> menu = List.of();

    @CreatedDate
    private LocalDateTime createdAt;
}
