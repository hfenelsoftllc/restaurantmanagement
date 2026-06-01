package com.hfenelsoftllc.foodcatalogue.dto;

import com.hfenelsoftllc.foodcatalogue.entity.FoodItemCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class FoodItemCatalogueDTO {
    @Schema(description = "Food item identifier", example = "f-101")
    private String id;

    @NotBlank(message = "Food item name is required")
    @Size(max = 120, message = "Food item name must not exceed 120 characters")
    @Schema(description = "Food item name", example = "Veggie Pizza")
    private String foodItemName;

    @NotBlank(message = "Food item description is required")
    @Size(max = 500, message = "Food item description must not exceed 500 characters")
    @Schema(description = "Food item description", example = "Stone-baked pizza with vegetables")
    private String foodItemDescription;

    @NotNull(message = "Food item price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Food item price must be greater than zero")
    @Schema(description = "Food item price", example = "15.99")
    private Double foodItemPrice;

    @NotNull(message = "Food item quantity is required")
    @Min(value = 0, message = "Food item quantity cannot be negative")
    @Schema(description = "Available quantity", example = "20")
    private Integer foodItemQuantity;

    @NotNull(message = "Food item category is required")
    @Schema(description = "Food category", example = "VEG")
    private FoodItemCategory foodItemCategory;

    @Size(max = 255, message = "Food item image URL must not exceed 255 characters")
    @Schema(description = "Food item image URL", example = "https://cdn.example.com/images/pizza.png")
    private String foodItemImage;

    @NotNull(message = "Restaurant id is required")
    @Positive(message = "Restaurant id must be greater than zero")
    @Schema(description = "Owning restaurant identifier", example = "1")
    private Long restaurantId;

    @Schema(description = "Creation timestamp")
    private Date createdAt;
}
