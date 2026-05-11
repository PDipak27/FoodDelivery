package com.dpp.fd.restaurant.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Embedded subdocument — not a top-level MongoDB collection. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuItem {
    private String itemId;
    private String name;
    private BigDecimal price;
    private boolean available;
}
