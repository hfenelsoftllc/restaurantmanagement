package com.hfenelsoftllc.restaurantlisting.controller;

import com.hfenelsoftllc.restaurantlisting.entity.Restaurant;
import com.hfenelsoftllc.restaurantlisting.repo.RestaurantRepo;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.service-registry.auto-registration.enabled=false",
        "integration.foodcatalogue.base-url=http://localhost:${wiremock.server.port}",
        "integration.foodcatalogue.use-load-balanced=false",
        "spring.datasource.url=jdbc:h2:mem:restaurantlisting-success-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RestaurantDetailsSuccessIntegrationTest {

    private static final WireMockServer WIREMOCK = new WireMockServer(options().dynamicPort());

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestaurantRepo restaurantRepo;

    private Long restaurantId;

    @BeforeAll
    static void startWireMock() {
        WIREMOCK.start();
        System.setProperty("wiremock.server.port", String.valueOf(WIREMOCK.port()));
    }

    @AfterAll
    static void stopWireMock() {
        WIREMOCK.stop();
        System.clearProperty("wiremock.server.port");
    }

    @BeforeEach
    void setup() {
        restaurantRepo.deleteAll();

        Restaurant restaurant = new Restaurant();
        restaurant.setName("Success Bistro");
        restaurant.setAddress("500 Happy Path Ave");
        restaurant.setCity("Mock City");
        restaurant.setState("MC");
        restaurant.setCountry("Stubland");
        restaurant.setPhone("+1-555-2000");
        restaurant.setEmail("success@example.com");
        restaurant.setDescription("Restaurant used for success integration test");

        restaurantId = restaurantRepo.save(restaurant).getId();

        WIREMOCK.resetAll();
        WIREMOCK.stubFor(get(urlEqualTo("/food-items/restaurant/" + restaurantId))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [
                                  {
                                    "id": "f-101",
                                    "foodItemName": "Test Burger",
                                    "foodItemDescription": "Burger from stub",
                                    "foodItemPrice": 12.5,
                                    "foodItemQuantity": 25,
                                    "foodItemCategory": "NON_VEG",
                                    "foodItemImage": "img.png",
                                    "restaurantId": %d
                                  }
                                ]
                                """.formatted(restaurantId))));
    }

    @Test
    void shouldReturnRestaurantDetailsWithMenuWhenFoodCatalogueIsReachable() throws Exception {
        mockMvc.perform(get("/restaurants/{id}/details", restaurantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restaurant.id").value(restaurantId))
                .andExpect(jsonPath("$.restaurant.name").value("Success Bistro"))
                .andExpect(jsonPath("$.menuItems").isArray())
                .andExpect(jsonPath("$.menuItems.length()").value(1))
                .andExpect(jsonPath("$.menuItems[0].id").value("f-101"))
                .andExpect(jsonPath("$.menuItems[0].foodItemName").value("Test Burger"))
                .andExpect(jsonPath("$.menuStatus").value("Menu retrieved"));
    }
}

