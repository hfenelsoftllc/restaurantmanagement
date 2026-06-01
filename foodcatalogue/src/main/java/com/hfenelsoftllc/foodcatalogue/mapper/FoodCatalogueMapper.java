package com.hfenelsoftllc.foodcatalogue.mapper;

import com.hfenelsoftllc.foodcatalogue.dto.FoodItemCatalogueDTO;
import com.hfenelsoftllc.foodcatalogue.entity.FoodItemCatalogue;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FoodCatalogueMapper {

    FoodItemCatalogue mapFoodCatalogueDTOtoFoodCatalogue(FoodItemCatalogueDTO foodCatalogueDTO);

    FoodItemCatalogueDTO mapFoodCataloguetoToFoodCatalogueDTO(FoodItemCatalogue foodCatalogue);
}
