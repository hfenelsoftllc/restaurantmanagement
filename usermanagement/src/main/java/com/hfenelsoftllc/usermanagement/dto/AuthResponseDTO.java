package com.hfenelsoftllc.usermanagement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class AuthResponseDTO {

    @Schema(description = "Authenticated user details")
    public UserDTO User;

    @Schema(description = "JWT bearer token generated for authenticated calls across services", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.signature")
    public String Token;
}

