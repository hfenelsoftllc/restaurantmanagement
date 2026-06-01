package com.hfenelsoftllc.usermanagement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class UserDTO {
    @Schema(description = "User identifier", example = "1")
    public Long Id;

    @NotBlank(message = "First name is required")
    @Size(max = 80, message = "First name must not exceed 80 characters")
    @Schema(description = "First name", example = "Jane")
    public String FirstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 80, message = "Last name must not exceed 80 characters")
    @Schema(description = "Last name", example = "Doe")
    public String LastName;

    @NotBlank(message = "Address is required")
    @Size(max = 255, message = "Address must not exceed 255 characters")
    @Schema(description = "Postal address", example = "42 Main Street")
    public String Address;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 150, message = "Email must not exceed 150 characters")
    @Schema(description = "User email address", example = "jane.doe@example.com")
    public String Email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @Schema(description = "User password", example = "SecurePass123!")
    public String Password;

    @NotBlank(message = "Confirm password is required")
    @Schema(description = "Password confirmation", example = "SecurePass123!")
    public String ConfirmPassword;
}
