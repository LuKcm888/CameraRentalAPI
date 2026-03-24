package com.camerarental.backend.security.request;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import static com.camerarental.backend.config.ValidationConstraints.*;


@Data
public class SignupRequest {
    @NotBlank
    @Size(min = USERNAME_MIN, max = USERNAME_MAX)
    private String username;

    @NotBlank
    @Email
    @Size(max = EMAIL_MAX)
    private String email;

    @NotBlank
    @Size(min = PASSWORD_MIN, max = DTO_PASSWORD_MAX)
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "Password must contain at least one uppercase, one lowercase, one digit, and one special character"
    )
    private String password;
}
