package com.hfenelsoftllc.usermanagement.service;

import com.hfenelsoftllc.usermanagement.dto.AuthResponseDTO;
import com.hfenelsoftllc.usermanagement.dto.LoginRequestDTO;
import com.hfenelsoftllc.usermanagement.dto.UserDTO;
import com.hfenelsoftllc.usermanagement.entity.User;
import com.hfenelsoftllc.usermanagement.exception.AuthenticationFailedException;
import com.hfenelsoftllc.usermanagement.mapper.UserMapper;
import com.hfenelsoftllc.usermanagement.repo.UserRepo;
import com.hfenelsoftllc.securitycommon.service.BearerTokenService;
import com.hfenelsoftllc.securitycommon.service.JwtTokenService;
import com.hfenelsoftllc.securitycommon.service.TokenClaims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepo userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private BearerTokenService bearerTokenService;

    @InjectMocks
    private UserService userService;

    @Test
    void addUserShouldCreateJwtUsingPersistedSessionVersion() {
        UserDTO request = new UserDTO();
        request.Email = "jane.doe@example.com";
        request.Password = "SecurePass123!";
        request.ConfirmPassword = "SecurePass123!";

        User mappedUser = new User();
        mappedUser.Email = request.Email;

        User savedUser = new User();
        savedUser.Id = 10L;
        savedUser.Email = request.Email;
        savedUser.Password = "hashed-password";

        UserDTO mappedResponse = new UserDTO();
        mappedResponse.Id = savedUser.Id;
        mappedResponse.Email = savedUser.Email;
        mappedResponse.Password = savedUser.Password;
        mappedResponse.ConfirmPassword = savedUser.Password;

        when(userMapper.mapUserDTOToUser(request)).thenReturn(mappedUser);
        when(passwordEncoder.encode(request.Password)).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userMapper.mapUserToUserDTO(savedUser)).thenReturn(mappedResponse);
        when(jwtTokenService.generateToken(any(Long.class), any(String.class), any(String.class))).thenReturn("jwt-token");

        AuthResponseDTO response = userService.addUser(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User persistedUser = userCaptor.getValue();

        assertEquals("hashed-password", persistedUser.Password);
        assertEquals("hashed-password", persistedUser.ConfirmPassword);
        assertTrue(persistedUser.SessionVersion != null && !persistedUser.SessionVersion.isBlank());
        verify(jwtTokenService).generateToken(savedUser.Id, savedUser.Email, persistedUser.SessionVersion);
        assertEquals("jwt-token", response.Token);
        assertEquals(savedUser.Id, response.User.Id);
        assertEquals(savedUser.Email, response.User.Email);
        assertNull(response.User.Password);
        assertNull(response.User.ConfirmPassword);
    }

    @Test
    void loginShouldRotateSessionVersionAndReturnFreshJwt() {
        LoginRequestDTO request = new LoginRequestDTO();
        request.Email = "jane.doe@example.com";
        request.Password = "SecurePass123!";

        User existingUser = new User();
        existingUser.Id = 5L;
        existingUser.Email = request.Email;
        existingUser.Password = "hashed-password";
        existingUser.SessionVersion = UUID.randomUUID().toString();

        UserDTO mappedResponse = new UserDTO();
        mappedResponse.Id = existingUser.Id;
        mappedResponse.Email = existingUser.Email;

        when(userRepository.findByEmailIgnoreCase(request.Email)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(request.Password, existingUser.Password)).thenReturn(true);
        when(userRepository.save(existingUser)).thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.mapUserToUserDTO(existingUser)).thenReturn(mappedResponse);
        when(jwtTokenService.generateToken(any(Long.class), any(String.class), any(String.class))).thenReturn("rotated-jwt-token");

        String oldSessionVersion = existingUser.SessionVersion;
        AuthResponseDTO response = userService.login(request);

        assertNotEquals(oldSessionVersion, existingUser.SessionVersion);
        verify(jwtTokenService).generateToken(existingUser.Id, existingUser.Email, existingUser.SessionVersion);
        assertEquals("rotated-jwt-token", response.Token);
        assertEquals(existingUser.Email, response.User.Email);
    }

    @Test
    void validateAccessTokenShouldRejectRotatedToken() {
        UUID currentSessionVersion = UUID.randomUUID();
        when(bearerTokenService.extractBearerToken("Bearer jwt-token")).thenReturn("jwt-token");
        when(jwtTokenService.parseClaims("jwt-token"))
                .thenReturn(new TokenClaims(7L, "jane.doe@example.com", currentSessionVersion.toString()));

        User user = new User();
        user.Id = 7L;
        user.Email = "jane.doe@example.com";
        user.SessionVersion = UUID.randomUUID().toString();

        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        AuthenticationFailedException exception = assertThrows(
                AuthenticationFailedException.class,
                () -> userService.validateAccessToken("Bearer jwt-token")
        );

        assertEquals("Token has been rotated or is no longer valid", exception.getMessage());
    }
}


