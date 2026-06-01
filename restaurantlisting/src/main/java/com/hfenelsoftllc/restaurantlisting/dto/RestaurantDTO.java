package com.hfenelsoftllc.restaurantlisting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RestaurantDTO {
    @Schema(description = "Unique restaurant identifier", example = "1")
    private Long id;

    @NotBlank(message = "Restaurant name is required")
    @Size(max = 120, message = "Restaurant name must not exceed 120 characters")
    @Schema(description = "Restaurant name", example = "Ocean Grill")
    private String name;

    @NotBlank(message = "Restaurant address is required")
    @Size(max = 255, message = "Restaurant address must not exceed 255 characters")
    @Schema(description = "Street address", example = "123 Beach Avenue")
    private String address;

    @NotBlank(message = "Restaurant city is required")
    @Size(max = 100, message = "Restaurant city must not exceed 100 characters")
    @Schema(description = "City name", example = "Miami")
    private String city;

    @Size(max = 100, message = "Restaurant state must not exceed 100 characters")
    @Schema(description = "State or province", example = "Florida")
    private String state;

    @NotBlank(message = "Restaurant country is required")
    @Size(max = 100, message = "Restaurant country must not exceed 100 characters")
    @Schema(description = "Country name", example = "USA")
    private String country;

    @Size(max = 30, message = "Restaurant phone must not exceed 30 characters")
    @Schema(description = "Restaurant contact phone", example = "+1-555-1000")
    private String phone;

    @Email(message = "Restaurant email must be a valid email address")
    @Size(max = 150, message = "Restaurant email must not exceed 150 characters")
    @Schema(description = "Restaurant email", example = "contact@oceangrill.example")
    private String email;

    @Size(max = 500, message = "Restaurant description must not exceed 500 characters")
    @Schema(description = "Restaurant description", example = "Seafood and grill specialties")
    private String description;
}
