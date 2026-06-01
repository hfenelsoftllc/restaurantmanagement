package com.hfenelsoftllc.restaurantlisting.controller;

import com.hfenelsoftllc.restaurantlisting.entity.Restaurant;
import com.hfenelsoftllc.restaurantlisting.repo.RestaurantRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.service-registry.auto-registration.enabled=false",
        "integration.foodcatalogue.base-url=http://127.0.0.1:65534",
        "spring.datasource.url=jdbc:h2:mem:restaurantlisting-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RestaurantDetailsFallbackIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestaurantRepo restaurantRepo;

    private Long restaurantId;

    @BeforeEach
    void setup() {
        restaurantRepo.deleteAll();

        Restaurant restaurant = new Restaurant();
        restaurant.setName("Fallback Bistro");
        restaurant.setAddress("123 Test St");
        restaurant.setCity("Test City");
        restaurant.setState("TS");
        restaurant.setCountry("Testland");
        restaurant.setPhone("+1-555-1000");
        restaurant.setEmail("fallback@example.com");
        restaurant.setDescription("Restaurant used for fallback integration test");

        restaurantId = restaurantRepo.save(restaurant).getId();
    }

    @Test
    void shouldReturnRestaurantDetailsWithFallbackMenuWhenFoodCatalogueIsUnavailable() throws Exception {
        mockMvc.perform(get("/restaurants/{id}/details", restaurantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restaurant.id").value(restaurantId))
                .andExpect(jsonPath("$.restaurant.name").value("Fallback Bistro"))
                .andExpect(jsonPath("$.menuItems").isArray())
                .andExpect(jsonPath("$.menuItems").isEmpty())
                .andExpect(jsonPath("$.menuStatus").value("Menu unavailable or empty"));
    }
}



