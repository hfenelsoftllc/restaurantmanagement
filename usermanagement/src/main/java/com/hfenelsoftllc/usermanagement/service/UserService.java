package com.hfenelsoftllc.usermanagement.service;

import com.hfenelsoftllc.usermanagement.dto.UserDTO;
import com.hfenelsoftllc.usermanagement.entity.User;
import com.hfenelsoftllc.usermanagement.exception.ResourceNotFoundException;
import com.hfenelsoftllc.usermanagement.exception.ServiceUnavailableException;
import com.hfenelsoftllc.usermanagement.mapper.UserMapper;
import com.hfenelsoftllc.usermanagement.repo.UserRepo;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {
    private final UserRepo userRepository;
    private final UserMapper userMapper;

    public UserService(UserRepo userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    public UserDTO addUser(UserDTO userDTO) {
        try {
            User savedUser = userRepository.save(userMapper.mapUserDTOToUser(userDTO));
            return userMapper.mapUserToUserDTO(savedUser);
        } catch (DataAccessException ex) {
            throw new ServiceUnavailableException("Unable to save user at the moment", ex);
        }
    }

    public List<UserDTO> findAllUsers() {
        try {
            List<User> users = userRepository.findAll();
            return users.stream().map(userMapper::mapUserToUserDTO).collect(Collectors.toList());
        } catch (DataAccessException ex) {
            throw new ServiceUnavailableException("Unable to fetch users at the moment", ex);
        }

    }

    public UserDTO findUserById(Long id) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found for id: " + id));
            return userMapper.mapUserToUserDTO(user);
        } catch (DataAccessException ex) {
            throw new ServiceUnavailableException("Unable to fetch user at the moment", ex);
        }

    }



}
