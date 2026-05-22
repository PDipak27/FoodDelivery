package com.dpp.fd.e2e;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

import static io.restassured.RestAssured.given;

/**
 * Base class shared by all E2E test suites.
 * <p>Configuration via system properties / env vars:
 * <ul>
 *   <li>{@code BASE_URL}   — gateway URL, default {@code http://localhost:8080}</li>
 * </ul>
 * <p>Run with the live stack up:
 * <pre>
 *   mvn test -pl e2e-tests -Dskip.e2e=false [-DBASE_URL=http://localhost:8080]
 * </pre>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseE2ETest {

    protected static final String BASE_URL =
            System.getenv("BASE_URL") != null ? System.getenv("BASE_URL")
            : System.getProperty("BASE_URL", "http://localhost:8080");

    @BeforeAll
    void configureRestAssured() {
        RestAssured.baseURI = BASE_URL;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    // --- DSL helpers ---

    protected RequestSpecification asGuest() {
        return given().contentType(ContentType.JSON);
    }

    protected RequestSpecification asUser(String token) {
        return given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token);
    }

    /** POST /auth/register → returns access token */
    protected String register(String email, String password, String role) {
        return given().contentType(ContentType.JSON)
                .body("""
                        {"email":"%s","password":"%s","role":"%s"}
                        """.formatted(email, password, role))
                .post("/auth/register")
                .then().statusCode(201)
                .extract().path("accessToken");
    }

    /** POST /auth/login → returns access token */
    protected String login(String email, String password) {
        return given().contentType(ContentType.JSON)
                .body("""
                        {"email":"%s","password":"%s"}
                        """.formatted(email, password))
                .post("/auth/login")
                .then().statusCode(200)
                .extract().path("accessToken");
    }

    /** Convenience: full response so callers can extract anything they need */
    protected Response loginResponse(String email, String password) {
        return given().contentType(ContentType.JSON)
                .body("""
                        {"email":"%s","password":"%s"}
                        """.formatted(email, password))
                .post("/auth/login")
                .then().statusCode(200)
                .extract().response();
    }
}
