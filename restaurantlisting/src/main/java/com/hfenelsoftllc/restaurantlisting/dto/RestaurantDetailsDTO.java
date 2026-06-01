package com.hfenelsoftllc.restaurantlisting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantDetailsDTO {
    @Schema(description = "Restaurant information")
    private RestaurantDTO restaurant;

    @Schema(description = "Menu items returned from the food catalogue service")
    private List<FoodItemCatalogueDTO> menuItems;

    @Schema(description = "Status of the menu lookup", example = "Menu retrieved")
    private String menuStatus;
}

