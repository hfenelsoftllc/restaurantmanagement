package com.hfenelsoftllc.restaurantlisting.controller;

import com.hfenelsoftllc.restaurantlisting.dto.RestaurantDTO;
import com.hfenelsoftllc.restaurantlisting.dto.RestaurantDetailsDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.hfenelsoftllc.restaurantlisting.service.RestaurantService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Restaurants", description = "Restaurant management endpoints")
@RestController
@RequestMapping("/restaurants")
public class RestaurantController {
    private final RestaurantService restaurantService;

    public RestaurantController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    @Operation(summary = "List restaurants", description = "Returns all registered restaurants")
    @ApiResponse(responseCode = "200", description = "Restaurants retrieved successfully")
    @GetMapping
    public ResponseEntity<List<RestaurantDTO>> getAllRestaurants() {
        return ResponseEntity.ok(restaurantService.findAllRestaurants());
    }

    @Operation(summary = "Create restaurant", description = "Creates a new restaurant entry")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Restaurant created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid restaurant payload"),
            @ApiResponse(responseCode = "503", description = "Service temporarily unavailable")
    })
    @PostMapping
    public ResponseEntity<RestaurantDTO> createRestaurant(@Valid @RequestBody RestaurantDTO restaurantDTO) {
        return ResponseEntity.status(201).body(restaurantService.addRestaurant(restaurantDTO));
    }

    @Operation(summary = "Get restaurant by id", description = "Returns a restaurant by its unique identifier")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Restaurant found"),
            @ApiResponse(responseCode = "404", description = "Restaurant not found"),
            @ApiResponse(responseCode = "503", description = "Service temporarily unavailable")
    })
    @GetMapping("/{id}")
    public ResponseEntity<RestaurantDTO> getRestaurantById(@PathVariable Long id) {
        return ResponseEntity.ok(restaurantService.fetchRestaurantById(id));
    }

    @Operation(summary = "Get restaurant details", description = "Returns restaurant information with menu details from the food catalogue service")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Restaurant details retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Restaurant not found"),
            @ApiResponse(responseCode = "503", description = "Dependent service temporarily unavailable")
    })
    @GetMapping("/{id}/details")
    public ResponseEntity<RestaurantDetailsDTO> getRestaurantDetails(@PathVariable Long id) {
        return ResponseEntity.ok(restaurantService.fetchRestaurantDetails(id));
    }

}
