package com.example.hubdemo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record SendCodeRequest(@Email @NotBlank String email) {
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 20) @Pattern(regexp = "^[A-Za-z0-9_-]+$") String username,
            @Email @NotBlank String email,
            @NotBlank @Size(min = 8, max = 128) String password
    ) {
    }

    public record LoginRequest(@Email @NotBlank String email, @NotBlank @Size(max = 128) String password) {
    }

    public record VerifyResetCodeRequest(@Email @NotBlank String email, @NotBlank @Size(min = 6, max = 6) String code) {
    }

    public record VerifyResetCodeResponse(String resetToken) {
    }

    public record ResetPasswordRequest(@Email @NotBlank String email, @NotBlank @Size(max = 128) String resetToken, @NotBlank @Size(min = 8, max = 128) String password) {
    }

    public record UpdateProfileUsernameRequest(@NotBlank @Size(min = 3, max = 20) @Pattern(regexp = "^[A-Za-z0-9_-]+$") String username) {
    }

    public record UpdateProfileEmailRequest(
            @Email @NotBlank String newEmail,
            @NotBlank @Size(min = 6, max = 6) String currentEmailCode,
            @NotBlank @Size(min = 6, max = 6) String newEmailCode
    ) {
    }

    public record UpdateProfilePasswordRequest(
            @NotBlank @Size(min = 6, max = 6) String emailCode,
            @NotBlank @Size(min = 8, max = 128) String password
    ) {
    }

    public record AuthResponse(Long id, String username, String email, String token, String role, String avatar, String adminPermissions) {
    }
}
