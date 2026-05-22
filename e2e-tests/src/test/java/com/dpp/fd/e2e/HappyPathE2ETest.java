package com.dpp.fd.e2e;

import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Full happy-path flow exercised against a live stack:
 *
 * 1.  Register a CUSTOMER and a RESTAURANT_OWNER
 * 2.  Owner creates a restaurant
 * 3.  Owner adds a menu to the restaurant
 * 4.  Customer places an order (payment amount ≤ 9999 → SUCCESS)
 * 5.  Owner/system advances status: PLACED → ACCEPTED → PREPARING → PICKED_UP → DELIVERED
 *
 * Each step asserts on HTTP status + key response fields so failures are
 * immediately obvious rather than producing a cryptic NPE later.
 *
 * Run: mvn test -pl e2e-tests -Dskip.e2e=false
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Happy Path — Full Order Lifecycle")
class HappyPathE2ETest extends BaseE2ETest {

    // Unique per run so the same DB can be used repeatedly without cleanup
    private final String suffix       = UUID.randomUUID().toString().substring(0, 8);
    private final String customerEmail = "customer_" + suffix + "@test.com";
    private final String ownerEmail    = "owner_"    + suffix + "@test.com";
    private final String password      = "Test@1234";

    private String customerToken;
    private String ownerToken;
    private String restaurantId;
    private String itemId;
    private String orderId;

    // ------------------------------------------------------------------ auth

    @Test @Order(1)
    @DisplayName("1 · Register customer")
    void registerCustomer() {
        customerToken = register(customerEmail, password, "CUSTOMER");
        assertNotNull(customerToken, "accessToken must not be null");
    }

    @Test @Order(2)
    @DisplayName("2 · Register restaurant owner")
    void registerOwner() {
        ownerToken = register(ownerEmail, password, "RESTAURANT_OWNER");
        assertNotNull(ownerToken, "accessToken must not be null");
    }

    @Test @Order(3)
    @DisplayName("3 · Login returns same token fields")
    void loginReturnsTokenFields() {
        Response res = loginResponse(customerEmail, password);
        assertNotNull(res.path("accessToken"),  "accessToken");
        assertNotNull(res.path("refreshToken"), "refreshToken");
        assertEquals("Bearer", res.path("tokenType"));
    }

    // ------------------------------------------------------------------ restaurant

    @Test @Order(4)
    @DisplayName("4 · Owner creates a restaurant")
    void createRestaurant() {
        Response res = asUser(ownerToken)
                .body("""
                        {"name":"Pizza Palace %s","cuisine":"Italian","city":"Mumbai"}
                        """.formatted(suffix))
                .post("/restaurants")
                .then()
                .statusCode(201)
                .body("id",      notNullValue())
                .body("open",  is(true))
                .body("cuisine", equalTo("Italian"))
                .extract().response();
        //res.asPrettyString()
        restaurantId = res.path("id");
        assertNotNull(restaurantId, "restaurantId must be present in response");
    }

    @Test @Order(5)
    @DisplayName("5 · Owner adds menu items")
    void addMenuItems() {
        Response res = asUser(ownerToken)
                .body("""
                        {"items":[
                          {"name":"Margherita","price":299.00,"available":true},
                          {"name":"Pepperoni", "price":399.00,"available":true}
                        ]}
                        """)
                .put("/restaurants/" + restaurantId + "/menu")
                .then()
                .statusCode(200)
                .body("menu",      hasSize(2))
                .body("menu[0].name", equalTo("Margherita"))
                .extract().response();

        itemId = res.path("menu[0].itemId");
        assertNotNull(itemId, "itemId must be returned in menu response");
    }

    @Test @Order(6) 
    @DisplayName("6 · Customer can browse restaurant list")
    void getRestaurants() {
        given().contentType("application/json")
                .get("/restaurants?city=Mumbai")
                .then()
                .statusCode(200)
                .body("$", not(empty()));
    }

    @Test @Order(7)
    @DisplayName("7 · Customer can fetch restaurant by id")
    void getRestaurantById() {
        asGuest()
                .get("/restaurants/" + restaurantId)
                .then()
                .statusCode(200)
                .body("id",     equalTo(restaurantId))
                .body("open", is(true));
    }

    // ------------------------------------------------------------------ order

    @Test @Order(8)
    @DisplayName("8 · Customer places order (payment succeeds ≤ 9999)")
    void placeOrder() {
        Response res = asUser(customerToken)
                .body("""
                        {"restaurantId":"%s","items":[{"itemId":"%s","quantity":2}]}
                        """.formatted(restaurantId, itemId))
                .post("/orders")
                .then()
                .statusCode(201)
                .body("id",          notNullValue())
                .body("status",      equalTo("PLACED"))
                .body("totalAmount", equalTo(598.0f))   // 299 × 2
                .extract().response();
res.asPrettyString();
        orderId = res.path("id");
        assertNotNull(orderId, "orderId must be returned");
    }

    @Test @Order(9)
    @DisplayName("9 · Customer can fetch their order")
    void getOrderById() {
        asUser(customerToken)
                .get("/orders/" + orderId)
                .then()
                .statusCode(200)
                .body("id",     equalTo(orderId))
                .body("status", equalTo("PLACED"));
    }

    // ------------------------------------------------------------------ status transitions

    @Test @Order(10)
    @DisplayName("10 · PLACED → ACCEPTED (delivery agent assigned)")
    void acceptOrder() {
        asUser(ownerToken)
                .body("""
                        {"status":"ACCEPTED"}
                        """)
                .patch("/orders/" + orderId + "/status")
                .then()
                .statusCode(200)
                .body("status", equalTo("ACCEPTED"));
    }

    @Test @Order(11)
    @DisplayName("11 · ACCEPTED → PREPARING")
    void prepareOrder() {
        asUser(ownerToken)
                .body("""
                        {"status":"PREPARING"}
                        """)
                .patch("/orders/" + orderId + "/status")
                .then()
                .statusCode(200)
                .body("status", equalTo("PREPARING"));
    }

    @Test @Order(12)
    @DisplayName("12 · PREPARING → PICKED_UP")
    void pickUpOrder() {
        asUser(ownerToken)
                .body("""
                        {"status":"PICKED_UP"}
                        """)
                .patch("/orders/" + orderId + "/status")
                .then()
                .statusCode(200)
                .body("status", equalTo("PICKED_UP"));
    }

    @Test @Order(13)
    @DisplayName("13 · Customer orders list includes the order")
    void customerOrdersList() {
        asUser(customerToken)
                .get("/orders/my")
                .then()
                .statusCode(200)
                .body("$",                not(empty()))
                .body("[0].id",           notNullValue());
    }
}
