package com.dpp.fd.order.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * RestClient (Spring 6.1) is the modern synchronous HTTP client — it replaces the
 * deprecated RestTemplate while avoiding the reactive overhead of WebClient.
 * Each downstream service gets its own RestClient bean with a pre-configured baseUrl.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient restaurantRestClient(@Value("${service.restaurant.url:http://localhost:8083}") String url) {
        return RestClient.builder().baseUrl(url).build();
    }

    @Bean
    public RestClient paymentRestClient(@Value("${service.payment.url:http://localhost:8085}") String url) {
        return RestClient.builder().baseUrl(url).build();
    }

    @Bean
    public RestClient notificationRestClient(@Value("${service.notification.url:http://localhost:8087}") String url) {
        return RestClient.builder().baseUrl(url).build();
    }

    @Bean
    public RestClient deliveryRestClient(@Value("${service.delivery.url:http://localhost:8086}") String url) {
        return RestClient.builder().baseUrl(url).build();
    }
}
