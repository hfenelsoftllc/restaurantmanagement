package com.hfenelsoftllc.foodcatalogue.service;

import com.hfenelsoftllc.foodcatalogue.dto.FoodItemCatalogueDTO;
import com.hfenelsoftllc.foodcatalogue.entity.FoodItemCatalogue;
import com.hfenelsoftllc.foodcatalogue.exception.ResourceNotFoundException;
import com.hfenelsoftllc.foodcatalogue.exception.ServiceUnavailableException;
import com.hfenelsoftllc.foodcatalogue.mapper.FoodCatalogueMapper;
import com.hfenelsoftllc.foodcatalogue.repo.FoodCatalogueRepo;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FoodCatalogueService {
    private final FoodCatalogueRepo foodCatalogueRepository;
    private final FoodCatalogueMapper foodCatalogueMapper;

    public FoodCatalogueService(FoodCatalogueRepo foodCatalogueRepository,
                                FoodCatalogueMapper foodCatalogueMapper) {
        this.foodCatalogueRepository = foodCatalogueRepository;
        this.foodCatalogueMapper = foodCatalogueMapper;
    }

    public FoodItemCatalogueDTO saveFoodItem(FoodItemCatalogueDTO foodItemCatalogueDTO){
        try {
            FoodItemCatalogue foodItemSaved = foodCatalogueRepository
                    .save(foodCatalogueMapper.mapFoodCatalogueDTOtoFoodCatalogue(foodItemCatalogueDTO));
            return foodCatalogueMapper.mapFoodCataloguetoToFoodCatalogueDTO(foodItemSaved);
        } catch (DataAccessException ex) {
            throw new ServiceUnavailableException("Unable to save food item at the moment", ex);
        }
    }

    public FoodItemCatalogueDTO findFoodItemById(String id) {
        try {
            FoodItemCatalogue foodItemCatalogue = foodCatalogueRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Food item not found for id: " + id));
            return foodCatalogueMapper.mapFoodCataloguetoToFoodCatalogueDTO(foodItemCatalogue);
        } catch (DataAccessException ex) {
            throw new ServiceUnavailableException("Unable to fetch food item at the moment", ex);
        }
    }

    public List<FoodItemCatalogueDTO> findFoodItemsByRestaurantId(Long restaurantId) {
        try {
            return foodCatalogueRepository.findByRestaurantId(restaurantId)
                    .stream()
                    .map(foodCatalogueMapper::mapFoodCataloguetoToFoodCatalogueDTO)
                    .collect(Collectors.toList());
        } catch (DataAccessException ex) {
            throw new ServiceUnavailableException("Unable to fetch restaurant menu at the moment", ex);
        }
    }

}
