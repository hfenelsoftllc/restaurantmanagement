package com.hfenelsoftllc.restaurantlisting.controllers;

import com.hfenelsoftllc.restaurantlisting.dtos.RestaurantDTO;
import com.hfenelsoftllc.restaurantlisting.entity.Restaurant;
import com.hfenelsoftllc.restaurantlisting.services.RestaurantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.imageio.spi.RegisterableService;
import java.util.List;

@RestController
@RequestMapping("/restaurant")
public class RestaurantController {
    @Autowired
    RestaurantService  restaurantService;

    @GetMapping("/fetchAllRestaurant")
    public ResponseEntity<List<RestaurantDTO>> fetchAllRestaurant(){
       List<RestaurantDTO> allRestaurant =  restaurantService.findAllRestaurants();
       return new ResponseEntity<>(allRestaurant, HttpStatus.OK);
    }

    @PostMapping("/addRestaurant")
    public ResponseEntity<RestaurantDTO> saveRestaurant(@RequestBody RestaurantDTO restaurantDTO){
        RestaurantDTO restaurantAdded = restaurantService.addRestaurant(restaurantDTO);
        return  new ResponseEntity<>(restaurantAdded, HttpStatus.CREATED);
    }

    @GetMapping("/fetchById/{id}")
    public  ResponseEntity<RestaurantDTO> fetchRestaurantById(@PathVariable Long id){
        return restaurantService.fetchRestaurantById(id);
    }

}
