package com.hfenelsoftllc.foodcatalogue.dto;

import com.hfenelsoftllc.foodcatalogue.entity.FoodItemCatalogue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FoodCataloguePage {
    private List<FoodItemCatalogue> foodItemCatalogueList;
}
