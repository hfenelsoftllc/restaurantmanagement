package com.hfenelsoftllc.restaurantlisting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FoodItemCatalogueDTO {
    @Schema(description = "Food item identifier", example = "f-101")
    private String id;

    @Schema(description = "Food item name", example = "Test Burger")
    private String foodItemName;

    @Schema(description = "Food item description", example = "Burger from stub")
    private String foodItemDescription;

    @Schema(description = "Food item price", example = "12.5")
    private Double foodItemPrice;

    @Schema(description = "Available quantity", example = "25")
    private Integer foodItemQuantity;

    @Schema(description = "Food category", example = "NON_VEG")
    private String foodItemCategory;

    @Schema(description = "Image URL or asset name", example = "img.png")
    private String foodItemImage;

    @Schema(description = "Owning restaurant identifier", example = "1")
    private Long restaurantId;
}

