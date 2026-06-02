package com.hfenelsoftllc.foodcatalogue.controller;

import com.hfenelsoftllc.foodcatalogue.dto.FoodItemCatalogueDTO;
import com.hfenelsoftllc.foodcatalogue.service.FoodCatalogueService;
import com.hfenelsoftllc.foodcatalogue.service.JwtAuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Food Catalogue", description = "Food item catalogue endpoints")
@RestController
@RequestMapping("/food-items")
public class FoodCatalogueController {
    private final FoodCatalogueService foodCatalogueService;
    private final JwtAuthenticationService jwtAuthenticationService;

    public FoodCatalogueController(FoodCatalogueService foodCatalogueService, JwtAuthenticationService jwtAuthenticationService) {
        this.foodCatalogueService = foodCatalogueService;
        this.jwtAuthenticationService = jwtAuthenticationService;
    }

    @Operation(summary = "Get food item by id", description = "Returns a food item by its unique identifier")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Food item found"),
            @ApiResponse(responseCode = "404", description = "Food item not found"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token"),
            @ApiResponse(responseCode = "503", description = "Service temporarily unavailable")
    })
    @GetMapping("/{id}")
    public ResponseEntity<FoodItemCatalogueDTO> getFoodItemById(
            @PathVariable String id,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        jwtAuthenticationService.validateAccessToken(authorizationHeader);
        return ResponseEntity.ok(foodCatalogueService.findFoodItemById(id));
    }

    @Operation(summary = "List menu items by restaurant", description = "Returns all food items for a specific restaurant")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Menu items retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token"),
            @ApiResponse(responseCode = "503", description = "Service temporarily unavailable")
    })
    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<List<FoodItemCatalogueDTO>> getFoodItemsByRestaurantId(
            @PathVariable Long restaurantId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        jwtAuthenticationService.validateAccessToken(authorizationHeader);
        return ResponseEntity.ok(foodCatalogueService.findFoodItemsByRestaurantId(restaurantId));
    }

    @Operation(summary = "Create food item", description = "Creates a new food item in the catalogue")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Food item created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid food item payload"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token"),
            @ApiResponse(responseCode = "503", description = "Service temporarily unavailable")
    })
    @PostMapping
    public ResponseEntity<FoodItemCatalogueDTO> createFoodItem(
            @Valid @RequestBody FoodItemCatalogueDTO foodItemCatalogueDTO,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        jwtAuthenticationService.validateAccessToken(authorizationHeader);
        return ResponseEntity.status(201).body(foodCatalogueService.saveFoodItem(foodItemCatalogueDTO));
    }

}
