package com.hfenelsoftllc.foodcatalogue.repo;


import com.hfenelsoftllc.foodcatalogue.entity.FoodItemCatalogue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FoodCatalogueRepo extends JpaRepository<FoodItemCatalogue, String> {
	List<FoodItemCatalogue> findByRestaurantId(Long restaurantId);
}
