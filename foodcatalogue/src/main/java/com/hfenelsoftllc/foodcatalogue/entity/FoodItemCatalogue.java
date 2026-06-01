package com.hfenelsoftllc.foodcatalogue.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor(force = true)
@Entity
@Table(name = "fooditem")
public class FoodItemCatalogue {

    @Id
    private String id;
    private String foodItemName;
    private String foodItemDescription;
    private Double foodItemPrice;
    private Integer foodItemQuantity;
    @Enumerated(EnumType.STRING)
    private FoodItemCategory foodItemCategory;
    private String foodItemImage;
    private Long restaurantId;
    private Date createdAt;

    @PrePersist
    void initializeCreatedAt() {
        if (createdAt == null) {
            createdAt = new Date();
        }
    }
}
