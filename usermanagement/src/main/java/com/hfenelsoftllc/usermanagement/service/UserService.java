package com.hfenelsoftllc.usermanagement.service;

import com.hfenelsoftllc.usermanagement.dto.AuthResponseDTO;
import com.hfenelsoftllc.usermanagement.dto.LoginRequestDTO;
import com.hfenelsoftllc.usermanagement.dto.UserDTO;
import com.hfenelsoftllc.usermanagement.entity.User;
import com.hfenelsoftllc.usermanagement.exception.AuthenticationFailedException;
import com.hfenelsoftllc.usermanagement.exception.ResourceNotFoundException;
import com.hfenelsoftllc.usermanagement.exception.ServiceUnavailableException;
import com.hfenelsoftllc.usermanagement.mapper.UserMapper;
import com.hfenelsoftllc.usermanagement.repo.UserRepo;
import com.hfenelsoftllc.securitycommon.service.BearerTokenService;
import com.hfenelsoftllc.securitycommon.service.JwtTokenService;
import com.hfenelsoftllc.securitycommon.service.TokenClaims;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {
    private final UserRepo userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final BearerTokenService bearerTokenService;

    public UserService(
            UserRepo userRepository,
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            BearerTokenService bearerTokenService
    ) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.bearerTokenService = bearerTokenService;
    }

    public AuthResponseDTO addUser(UserDTO userDTO) {
        if (!userDTO.Password.equals(userDTO.ConfirmPassword)) {
            throw new IllegalArgumentException("Password and confirm password do not match");
        }

        try {
            User userToSave = userMapper.mapUserDTOToUser(userDTO);
            String passwordHash = passwordEncoder.encode(userDTO.Password);
            UUID sessionVersion = UUID.randomUUID();
            userToSave.Password = passwordHash;
            userToSave.ConfirmPassword = passwordHash;
            userToSave.SessionVersion = sessionVersion.toString();

            User savedUser = userRepository.save(userToSave);
            String token = jwtTokenService.generateToken(savedUser.Id, savedUser.Email, sessionVersion.toString());
            return new AuthResponseDTO(hideSensitiveFields(userMapper.mapUserToUserDTO(savedUser)), token);
        } catch (DataAccessException ex) {
            throw new ServiceUnavailableException("Unable to save user at the moment", ex);
        }
    }

    public List<UserDTO> findAllUsers() {
        try {
            List<User> users = userRepository.findAll();
            return users.stream()
                    .map(userMapper::mapUserToUserDTO)
                    .map(this::hideSensitiveFields)
                    .collect(Collectors.toList());
        } catch (DataAccessException ex) {
            throw new ServiceUnavailableException("Unable to fetch users at the moment", ex);
        }

    }

    public UserDTO findUserById(Long id) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found for id: " + id));
            return hideSensitiveFields(userMapper.mapUserToUserDTO(user));
        } catch (DataAccessException ex) {
            throw new ServiceUnavailableException("Unable to fetch user at the moment", ex);
        }

    }

    public AuthResponseDTO login(LoginRequestDTO loginRequestDTO) {
        try {
            User user = userRepository.findByEmailIgnoreCase(loginRequestDTO.Email)
                    .orElseThrow(() -> new AuthenticationFailedException("Invalid email or password"));

            // BCrypt stores the salt in the hash string; matches() re-hashes and compares safely.
            if (!passwordEncoder.matches(loginRequestDTO.Password, user.Password)) {
                throw new AuthenticationFailedException("Invalid email or password");
            }

            UUID rotatedSessionVersion = UUID.randomUUID();
            user.SessionVersion = rotatedSessionVersion.toString();
            User savedUser = userRepository.save(user);
            String token = jwtTokenService.generateToken(savedUser.Id, savedUser.Email, rotatedSessionVersion.toString());

            return new AuthResponseDTO(hideSensitiveFields(userMapper.mapUserToUserDTO(savedUser)), token);
        } catch (DataAccessException ex) {
            throw new ServiceUnavailableException("Unable to login at the moment", ex);
        }
    }

    public void validateAccessToken(String authorizationHeader) {
        try {
            String bearerToken = bearerTokenService.extractBearerToken(authorizationHeader);
            TokenClaims claims = jwtTokenService.parseClaims(bearerToken);
            Long userId = claims.userId();
            String sessionVersion = claims.sessionVersion();

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new AuthenticationFailedException("Invalid token"));

            if (user.SessionVersion == null || !user.SessionVersion.equals(sessionVersion)) {
                throw new AuthenticationFailedException("Token has been rotated or is no longer valid");
            }
        } catch (IllegalArgumentException ex) {
            throw new AuthenticationFailedException("Invalid or expired token");
        } catch (DataAccessException ex) {
            throw new ServiceUnavailableException("Unable to validate token at the moment", ex);
        }
    }


    private UserDTO hideSensitiveFields(UserDTO userDTO) {
        userDTO.Password = null;
        userDTO.ConfirmPassword = null;
        return userDTO;
    }



}
