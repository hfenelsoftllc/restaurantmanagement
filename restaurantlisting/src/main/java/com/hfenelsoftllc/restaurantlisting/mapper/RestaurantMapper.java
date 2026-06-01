package com.hfenelsoftllc.restaurantlisting.mapper;

import com.hfenelsoftllc.restaurantlisting.dto.RestaurantDTO;
import com.hfenelsoftllc.restaurantlisting.entity.Restaurant;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RestaurantMapper {
    Restaurant mapRestaurantDTOToRestaurant(RestaurantDTO restaurantDTO);

    RestaurantDTO mapRestaurantToRestaurantDTO(Restaurant restaurant);
}

