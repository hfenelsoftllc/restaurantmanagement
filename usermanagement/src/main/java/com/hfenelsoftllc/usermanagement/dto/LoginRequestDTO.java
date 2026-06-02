package com.hfenelsoftllc.usermanagement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class LoginRequestDTO {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Schema(description = "User email address", example = "jane.doe@example.com")
    public String Email;

    @NotBlank(message = "Password is required")
    @Schema(description = "Raw password used for authentication", example = "SecurePass123!")
    public String Password;
}

