package com.ecommerce.api_gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class ApiGatewayRoutingTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void routeToProductService_ShouldForward() {
        webTestClient.get().uri("/products")
                .exchange()
                .expectStatus().is5xxServerError(); // 503 because service is not running
    }

    @Test
    void routeToCategoryService_ShouldForward() {
        webTestClient.get().uri("/categories")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void routeToInventoryService_ShouldForward() {
        webTestClient.get().uri("/inventory")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void routeToUnknown_ShouldReturnNotFound() {
        webTestClient.get().uri("/unknown")
                .exchange()
                .expectStatus().isNotFound();
    }
}
