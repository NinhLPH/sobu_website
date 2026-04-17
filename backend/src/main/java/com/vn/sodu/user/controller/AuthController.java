package com.vn.sodu.user.controller;

import com.vn.sodu.user.dto.*;
import com.vn.sodu.global.dto.ApiResponseDTO;
import com.vn.sodu.user.dto.*;
import com.vn.sodu.user.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Authentication and authorization endpoints for user login, registration, and token management")
@SuppressWarnings("rawtypes")
public class AuthController {

    private final AuthService authService;

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
        try {
            log.info("Login request for email: {}", loginRequest.getEmail());
            LoginResponse response = authService.login(loginRequest);
            return ResponseEntity.ok(
                    ApiResponseDTO.success(response, "Login successful", HttpStatus.OK.value())
            );
        } catch (Exception e) {
            log.error("Login error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponseDTO.error("Login failed", e.getMessage(), HttpStatus.UNAUTHORIZED.value()));
        }
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
        try {
            log.info("Refresh token request");
            LoginResponse response = authService.refreshToken(request);
            return ResponseEntity.ok(
                    ApiResponseDTO.success(response, "Token refreshed successfully", HttpStatus.OK.value())
            );
        } catch (Exception e) {
            log.error("Refresh token error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponseDTO.error("Refresh token failed", e.getMessage(), HttpStatus.UNAUTHORIZED.value()));
        }
    }

    @PostMapping("/register")
    @Operation(
        summary = "User registration",
        description = "Registers a new user account. User will receive an activation email."
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
        try {
            log.info("Register request for email: {}", registerRequest.getEmail());
            RegisterResponse response = authService.register(registerRequest);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponseDTO.success(response, "Registration successful", HttpStatus.CREATED.value()));
        } catch (Exception e) {
            log.error("Registration error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponseDTO.error("Registration failed", e.getMessage(), HttpStatus.BAD_REQUEST.value()));
        }
    }

    @GetMapping("/activate")
    @Operation(
        summary = "Activate user account",
        description = "Activates a user account using the activation token sent via email"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Account activated successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Activation failed - invalid or expired token",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<?> activate(
        @Parameter(description = "Activation token from email", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
        @RequestParam("token") String token) {
        try {
            log.info("Activation request for token: {}", token);
            RegisterResponse response = authService.activateAccount(token);
            return ResponseEntity.ok(ApiResponseDTO.success(response, "Account activated", HttpStatus.OK.value()));
        } catch (Exception e) {
            log.error("Activation error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponseDTO.error("Activation failed", e.getMessage(), HttpStatus.BAD_REQUEST.value()));
        }
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
        @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            log.info("Logout request");
            
            String token = null;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
            
            authService.logout(token);
            
            return ResponseEntity.ok(
                    ApiResponseDTO.success(null, "Logout successful", HttpStatus.OK.value())
            );
        } catch (Exception e) {
            log.error("Logout error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponseDTO.error("Logout failed", e.getMessage(), HttpStatus.BAD_REQUEST.value()));
        }
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
