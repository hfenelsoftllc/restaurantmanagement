package com.hfenelsoftllc.restaurantlisting.service;

import com.hfenelsoftllc.restaurantlisting.dto.FoodItemCatalogueDTO;
import com.hfenelsoftllc.restaurantlisting.dto.RestaurantDTO;
import com.hfenelsoftllc.restaurantlisting.dto.RestaurantDetailsDTO;
import com.hfenelsoftllc.restaurantlisting.entity.Restaurant;
import com.hfenelsoftllc.restaurantlisting.exception.ResourceNotFoundException;
import com.hfenelsoftllc.restaurantlisting.exception.ServiceUnavailableException;
import com.hfenelsoftllc.restaurantlisting.mapper.RestaurantMapper;
import com.hfenelsoftllc.restaurantlisting.repo.RestaurantRepo;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.stream.Collectors;

@Service
public class RestaurantService {
    private final RestaurantRepo restaurantRepo;
    private final RestaurantMapper restaurantMapper;
    private final FoodCatalogueClientService foodCatalogueClientService;

    public RestaurantService(
            RestaurantRepo restaurantRepo,
            RestaurantMapper restaurantMapper,
            FoodCatalogueClientService foodCatalogueClientService
    ) {
        this.restaurantRepo = restaurantRepo;
        this.restaurantMapper = restaurantMapper;
        this.foodCatalogueClientService = foodCatalogueClientService;
    }


    public List<RestaurantDTO> findAllRestaurants() {
        try {
            List<Restaurant> restaurants = restaurantRepo.findAll();
            return restaurants.stream().map(
                            restaurantMapper::mapRestaurantToRestaurantDTO)
                    .collect(Collectors.toList());
        } catch (DataAccessException ex) {
            throw new ServiceUnavailableException("Unable to fetch restaurants at the moment", ex);
        }
    }

    public RestaurantDTO addRestaurant(RestaurantDTO restaurantDTO) {
        try {
            Restaurant savedRestaurant = restaurantRepo.save(
                    restaurantMapper.mapRestaurantDTOToRestaurant(restaurantDTO));
            return restaurantMapper.mapRestaurantToRestaurantDTO(savedRestaurant);
        } catch (DataAccessException ex) {
            throw new ServiceUnavailableException("Unable to save restaurant at the moment", ex);
        }
    }

    public RestaurantDTO fetchRestaurantById(Long id) {
        try {
            Restaurant restaurant = restaurantRepo.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found for id: " + id));
            return restaurantMapper.mapRestaurantToRestaurantDTO(restaurant);
        } catch (DataAccessException ex) {
            throw new ServiceUnavailableException("Unable to fetch restaurant at the moment", ex);
        }
    }

    public RestaurantDetailsDTO fetchRestaurantDetails(Long id) {
        RestaurantDTO restaurant = fetchRestaurantById(id);
        List<FoodItemCatalogueDTO> menuItems = foodCatalogueClientService.getMenuByRestaurantId(id);

        String menuStatus = menuItems.isEmpty()
                ? "Menu unavailable or empty"
                : "Menu retrieved";

        return new RestaurantDetailsDTO(restaurant, menuItems, menuStatus);
    }

}
