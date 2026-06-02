package com.hfenelsoftllc.usermanagement.controller;

import com.hfenelsoftllc.usermanagement.dto.AuthResponseDTO;
import com.hfenelsoftllc.usermanagement.dto.UserDTO;
import com.hfenelsoftllc.usermanagement.dto.LoginRequestDTO;
import com.hfenelsoftllc.usermanagement.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@Tag(name = "Users", description = "User management endpoints")
@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "List users", description = "Returns all registered users")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid token")
    })
    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers(@RequestHeader("Authorization") String authorizationHeader) {
        userService.validateAccessToken(authorizationHeader);
        return ResponseEntity.ok(userService.findAllUsers());
    }

    @Operation(summary = "Get user by id", description = "Returns a user by its unique identifier")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "401", description = "Invalid token"),
            @ApiResponse(responseCode = "503", description = "Service temporarily unavailable")
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id, @RequestHeader("Authorization") String authorizationHeader) {
        userService.validateAccessToken(authorizationHeader);
        return ResponseEntity.ok(userService.findUserById(id));
    }

    @Operation(summary = "Create user", description = "Creates a new user account")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid user payload"),
            @ApiResponse(responseCode = "503", description = "Service temporarily unavailable")
    })
    @PostMapping
    public ResponseEntity<AuthResponseDTO> createUser(@Valid @RequestBody UserDTO userDTO) {
        return ResponseEntity.status(201).body(userService.addUser(userDTO));
    }

    @Operation(summary = "Login user", description = "Authenticates user by email and password, then rotates and returns a fresh JWT")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentication successful"),
            @ApiResponse(responseCode = "401", description = "Invalid email or password"),
            @ApiResponse(responseCode = "503", description = "Service temporarily unavailable")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequestDTO) {
        return ResponseEntity.ok(userService.login(loginRequestDTO));
    }

    @Operation(summary = "Validate token", description = "Validates that a bearer token is well-formed, active, and not rotated")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Token is valid"),
            @ApiResponse(responseCode = "401", description = "Token is invalid or expired"),
            @ApiResponse(responseCode = "503", description = "Service temporarily unavailable")
    })
    @GetMapping("/token/validate")
    public ResponseEntity<Void> validateToken(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        userService.validateAccessToken(authorizationHeader);
        return ResponseEntity.noContent().build();
    }
}

