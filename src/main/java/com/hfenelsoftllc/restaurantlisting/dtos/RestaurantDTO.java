package com.hfenelsoftllc.restaurantlisting.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RestaurantDTO {
    private Long id;
    private String name;
    private String address;
    private String city;
    private String state;
    private String country;
    private String phone;
    private String email;
    private String description;
}
