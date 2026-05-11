/**
 * MongoDB init script — runs automatically on first container startup
 * via /docker-entrypoint-initdb.d convention (only when data volume is empty).
 *
 * What it does:
 *  - Switches to the fooddelivery_restaurant database (creates it implicitly)
 *  - Creates the restaurants collection with a JSON Schema validator
 *  - Adds indexes used by RestaurantRepository query methods
 */

db = db.getSiblingDB("fooddelivery_restaurant");

// Create collection with basic schema validation
db.createCollection("restaurants", {
    validator: {
        $jsonSchema: {
            bsonType: "object",
            required: ["ownerId", "name", "cuisine", "city", "isOpen"],
            properties: {
                ownerId:  { bsonType: "string", description: "UUID of the owning user" },
                name:     { bsonType: "string" },
                cuisine:  { bsonType: "string" },
                city:     { bsonType: "string" },
                isOpen:   { bsonType: "bool" },
                menu:     { bsonType: "array" }
            }
        }
    },
    validationAction: "warn"    // warn rather than reject, keeps dev iteration smooth
});

// Indexes mirror RestaurantRepository query methods
db.restaurants.createIndex({ city: 1, isOpen: 1 },          { name: "idx_city_open" });
db.restaurants.createIndex({ cuisine: 1, isOpen: 1 },        { name: "idx_cuisine_open" });
db.restaurants.createIndex({ ownerId: 1 },                   { name: "idx_owner" });

print("MongoDB init complete: fooddelivery_restaurant database and indexes created.");
