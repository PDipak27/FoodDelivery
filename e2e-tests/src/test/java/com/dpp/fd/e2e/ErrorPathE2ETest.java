package com.dpp.fd.e2e;

import org.junit.jupiter.api.*;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Negative / error-path scenarios:
 *
 *  A. Duplicate registration is rejected (409 / 400)
 *  B. Bad credentials rejected (401)
 *  C. Order against a closed restaurant rejected
 *  D. Order with amount > 9999 → PAYMENT_FAILED status
 *  E. Invalid status transition rejected (e.g. PLACED → DELIVERED)
 *  F. Unauthenticated request rejected (401)
 *  G. Unknown restaurant returns 404
 *
 * Run: mvn test -pl e2e-tests -Dskip.e2e=false
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Error Paths — Validation & Business Rule Failures")
class ErrorPathE2ETest extends BaseE2ETest {

    private final String suffix    = UUID.randomUUID().toString().substring(0, 8);
    private final String email     = "err_user_" + suffix + "@test.com";
    private final String ownerEmail = "err_owner_" + suffix + "@test.com";
    private final String password  = "Test@1234";

    private String customerToken;
    private String ownerToken;
    private String openRestaurantId;    // stays open — used for payment failure test
    private String closedRestaurantId;  // will be toggled closed
    private String expensiveItemId;     // price set above 9999 threshold
    private String cheapItemId;         // price ≤ 9999 — used for FSM transition test

    // ------------------------------------------------------------------ setup accounts & restaurant

    @Test @Order(1)
    @DisplayName("A1 · Setup — register customer & owner")
    void setup() {
        customerToken = register(email, password, "CUSTOMER");
        ownerToken    = register(ownerEmail, password, "RESTAURANT_OWNER");
    }

    @Test @Order(2)
    @DisplayName("A2 · Setup — create open restaurant with expensive item")
    void setupOpenRestaurant() {
        openRestaurantId = asUser(ownerToken)
                .body("""
                        {"name":"Expensive Eats %s","cuisine":"French","city":"Delhi"}
                        """.formatted(suffix))
                .post("/restaurants")
                .then().statusCode(201)
                .extract().path("id");

        // Add one item above the payment threshold and one cheap item for FSM tests
        var menuRes = asUser(ownerToken)
                .body("""
                        {"items":[
                          {"name":"Gold Truffle","price":10000.00,"available":true},
                          {"name":"House Salad", "price":199.00, "available":true}
                        ]}
                        """)
                .put("/restaurants/" + openRestaurantId + "/menu")
                .then().statusCode(200)
                .extract().response();

        expensiveItemId = menuRes.path("menu[0].itemId");
        cheapItemId     = menuRes.path("menu[1].itemId");
    }

    @Test @Order(3)
    @DisplayName("A3 · Setup — create restaurant and close it")
    void setupClosedRestaurant() {
        closedRestaurantId = asUser(ownerToken)
                .body("""
                        {"name":"Closed Kitchen %s","cuisine":"Indian","city":"Delhi"}
                        """.formatted(suffix))
                .post("/restaurants")
                .then().statusCode(201)
                .extract().path("id");

        // Toggle it closed (controller maps to /toggle, not /toggle-open)
        asUser(ownerToken)
                .patch("/restaurants/" + closedRestaurantId + "/toggle")
                .then().statusCode(200)
                .body("open", is(false));
    }

    // ------------------------------------------------------------------ auth errors

    @Test @Order(10)
    @DisplayName("B · Duplicate registration is rejected")
    void duplicateRegistrationRejected() {
        given().contentType("application/json")
                .body("""
                        {"email":"%s","password":"%s","role":"CUSTOMER"}
                        """.formatted(email, password))
                .post("/auth/register")
                .then()
                .statusCode(anyOf(is(400),is(401), is(409)));
    }

    @Test @Order(11)
    @DisplayName("C · Wrong password returns 401")
    void wrongPasswordRejected() {
        given().contentType("application/json")
                .body("""
                        {"email":"%s","password":"wrongpass"}
                        """.formatted(email))
                .post("/auth/login")
                .then()
                .statusCode(401);
    }

    @Test @Order(12)
    @DisplayName("D · Unknown email returns 401")
    void unknownEmailRejected() {
        given().contentType("application/json")
                .body("""
                        {"email":"nobody@nowhere.com","password":"any"}
                        """)
                .post("/auth/login")
                .then()
                .statusCode(401);
    }

    // ------------------------------------------------------------------ restaurant errors

    @Test @Order(20)
    @DisplayName("E · Fetch unknown restaurant returns 404")
    void unknownRestaurant404() {
        asGuest()
                .get("/restaurants/" + UUID.randomUUID())
                .then()
                .statusCode(404);
    }

    @Test @Order(21)
    @DisplayName("F · Non-owner cannot update menu (403)")
    void nonOwnerCannotUpdateMenu() {
        asUser(customerToken)
                .body("""
                        {"items":[{"name":"Hack","price":1.00,"available":true}]}
                        """)
                .put("/restaurants/" + openRestaurantId + "/menu")
                .then()
                .statusCode(403);
    }

    // ------------------------------------------------------------------ order errors

    @Test @Order(30)
    @DisplayName("G · Order against closed restaurant is rejected")
    void orderClosedRestaurantRejected() {
        asUser(customerToken)
                .body("""
                        {"restaurantId":"%s","items":[{"itemId":"fake-id","quantity":1}]}
                        """.formatted(closedRestaurantId))
                .post("/orders")
                .then()
                .statusCode(anyOf(is(400), is(409), is(422)));
    }

    @Test @Order(31)
    @DisplayName("H · Order with amount > 9999 results in PAYMENT_FAILED")
    void paymentFailedForHighAmount() {
        // price=10000 × qty=1 → 10000 > threshold 9999 → PAYMENT_FAILED
        // Order is still persisted (201 CREATED) but with PAYMENT_FAILED status
        asUser(customerToken)
                .body("""
                        {"restaurantId":"%s","items":[{"itemId":"%s","quantity":1}]}
                        """.formatted(openRestaurantId, expensiveItemId))
                .post("/orders")
                .then()
                .statusCode(201)
                .body("status", equalTo("PAYMENT_FAILED"));
    }

    @Test @Order(32)
    @DisplayName("I · Invalid status transition PLACED → DELIVERED is rejected")
    void invalidStatusTransitionRejected() {
        // Place a cheap order so it lands in PLACED status (payment succeeds)
        String newOrderId = asUser(customerToken)
                .body("""
                        {"restaurantId":"%s","items":[{"itemId":"%s","quantity":1}]}
                        """.formatted(openRestaurantId, cheapItemId))
                .post("/orders")
                .then()
                .statusCode(201)
                .body("status", equalTo("PLACED"))
                .extract().path("id");

        // Try illegal jump: PLACED → DELIVERED (skips ACCEPTED, PREPARING, PICKED_UP)
        asUser(ownerToken)
                .body("""
                        {"status":"DELIVERED"}
                        """)
                .patch("/orders/" + newOrderId + "/status")
                .then()
                .statusCode(anyOf(is(400), is(409), is(422)));
    }

    // ------------------------------------------------------------------ auth guard

    @Test @Order(40)
    @DisplayName("J · Unauthenticated request to protected endpoint returns 401")
    void unauthenticatedRequestRejected() {
        asGuest()
                .get("/orders/my")
                .then()
                .statusCode(401);
    }

    @Test @Order(41)
    @DisplayName("K · Invalid / tampered token returns 401")
    void invalidTokenRejected() {
        asUser("this.is.not.a.valid.jwt")
                .get("/orders/my")
                .then()
                .statusCode(401);
    }
}
