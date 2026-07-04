package com.vn.sodu.user.controller;

import com.vn.sodu.user.dto.*;
import com.vn.sodu.global.dto.ApiResponseDTO;
import com.vn.sodu.user.service.AuthService;
import com.vn.sodu.user.service.OAuthService;
import com.vn.sodu.user.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/auth", "/auth"})
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Authentication and authorization endpoints for user login, registration, and token management")
@SuppressWarnings("rawtypes")
public class AuthController {

    private final AuthService authService;
    private final OAuthService oAuthService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/login")
    @Operation(
        summary = "User login",
        description = "Authenticates a user with email and password, returns JWT access token and refresh token"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login successful",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Invalid email or password",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    @RequestBody(
        description = "Email and password credentials",
        required = true,
        content = @Content(schema = @Schema(implementation = LoginRequest.class))
    )
    public ResponseEntity<?> login(@org.springframework.web.bind.annotation.RequestBody LoginRequest loginRequest) {
        log.info("Login request for email: {}", loginRequest.getEmail());
        LoginResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(
                ApiResponseDTO.success(response, "Login successful", HttpStatus.OK.value())
        );
    }

    @PostMapping("/oauth/google")
    @Operation(
        summary = "Google OAuth login",
        description = "Authenticates a user via Google ID token, creates or links an account if necessary, and returns JWT tokens"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Google login successful",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Invalid Google ID token",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<?> googleLogin(@org.springframework.web.bind.annotation.RequestBody GoogleLoginRequest request) {
        log.info("Google OAuth login request");
        LoginResponse response = oAuthService.loginWithGoogle(request);
        return ResponseEntity.ok(
                ApiResponseDTO.success(response, "Google login successful", HttpStatus.OK.value())
        );
    }

    @PostMapping("/refresh-token")
    @Operation(
        summary = "Refresh access token",
        description = "Generates a new access token using a valid refresh token"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token refreshed successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    @RequestBody(
        description = "Refresh token",
        required = true,
        content = @Content(schema = @Schema(implementation = RefreshTokenRequest.class))
    )
    public ResponseEntity<?> refreshToken(@org.springframework.web.bind.annotation.RequestBody RefreshTokenRequest request) {
        log.info("Refresh token request");
        LoginResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(
                ApiResponseDTO.success(response, "Token refreshed successfully", HttpStatus.OK.value())
        );
    }

    @PostMapping("/register")
    @Operation(
        summary = "User registration",
        description = "Registers a new user account."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Registration successful",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Registration failed - email already exists or invalid data",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    @RequestBody(
        description = "User registration details",
        required = true,
        content = @Content(schema = @Schema(implementation = RegisterRequest.class))
    )
    public ResponseEntity<?> register(@org.springframework.web.bind.annotation.RequestBody RegisterRequest registerRequest) {
        log.info("Register request for email: {}", registerRequest.getEmail());
        RegisterResponse response = authService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success(response, "Registration successful", HttpStatus.CREATED.value()));
    }

    @PostMapping("/logout")
    @Operation(
        summary = "User logout",
        description = "Logs out the current user and invalidates their session"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Logout successful",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Logout failed",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<?> logout(
        @Parameter(description = "JWT token in Bearer format", example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        @RequestHeader(value = "Authorization", required = false) String authHeader,
        @org.springframework.web.bind.annotation.RequestBody(required = false) RefreshTokenRequest request) {
        
        log.info("Logout request");
        
        String accessToken = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7);
        }

        String refreshToken = request != null ? request.getRefreshToken() : null;
        
        authService.logout(accessToken, refreshToken);
        
        return ResponseEntity.ok(
                ApiResponseDTO.success(null, "Logout successful", HttpStatus.OK.value())
        );
    }

    @PostMapping("/forgot-password")
    @Operation(
        summary = "Request password reset",
        description = "Sends a password reset link to the specified email if the account exists"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Reset email sent (or email not found — same response for security)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Cooldown not expired or invalid email",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<?> forgotPassword(@org.springframework.web.bind.annotation.RequestBody @Valid ForgotPasswordRequest request) {
        log.info("Forgot password request for email: {}", request.getEmail());
        passwordResetService.forgotPassword(request);
        return ResponseEntity.ok(
                ApiResponseDTO.success(null, "If the email exists, a reset link has been sent", HttpStatus.OK.value())
        );
    }

    @PostMapping("/reset-password")
    @Operation(
        summary = "Reset password with token",
        description = "Resets the user's password using a valid reset token"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Password reset successful",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid, expired, or already used token",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<?> resetPassword(@org.springframework.web.bind.annotation.RequestBody @Valid ResetPasswordRequest request) {
        log.info("Password reset request");
        passwordResetService.resetPassword(request);
        return ResponseEntity.ok(
                ApiResponseDTO.success(null, "Password reset successful", HttpStatus.OK.value())
        );
    }

    @GetMapping("/health")
    @Operation(
        summary = "Health check",
        description = "Verifies that the authentication service is running"
    )
    @ApiResponse(responseCode = "200", description = "Service is healthy",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class)))
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(
                ApiResponseDTO.success("Auth service is running", "OK", HttpStatus.OK.value())
        );
    }
}
