package com.hfenelsoftllc.usermanagement.mapper;

import com.hfenelsoftllc.usermanagement.dto.UserDTO;
import com.hfenelsoftllc.usermanagement.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;


@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    User mapUserDTOToUser(UserDTO userDTO);

    UserDTO mapUserToUserDTO(User user);
}
