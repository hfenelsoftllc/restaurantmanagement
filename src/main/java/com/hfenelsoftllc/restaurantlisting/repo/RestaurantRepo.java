package com.hfenelsoftllc.restaurantlisting.repo;

import com.hfenelsoftllc.restaurantlisting.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RestaurantRepo extends JpaRepository<Restaurant, Long> {
//    verride
//    List<Restaurant> findAll(); @O
}
