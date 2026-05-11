package com.dpp.fd.restaurant.integration;

import com.dpp.fd.restaurant.dto.CreateRestaurantRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests against a real MongoDB container via Testcontainers.
 * Redis caching is disabled via application-test.yml (cache.type: none).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class RestaurantIntegrationTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7");

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void createThenGet_returnsCorrectData() throws Exception {
        CreateRestaurantRequest req = new CreateRestaurantRequest();
        req.setName("Test Kitchen"); req.setCuisine("Indian"); req.setCity("Pune");

        String response = mockMvc.perform(post("/restaurants")
                        .header("X-User-Id", "owner-123")
                        .header("X-User-Role", "RESTAURANT_OWNER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Kitchen"))
                .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(get("/restaurants/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cuisine").value("Indian"));
    }

    @Test
    void getAll_returnsOpenRestaurants() throws Exception {
        mockMvc.perform(get("/restaurants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
