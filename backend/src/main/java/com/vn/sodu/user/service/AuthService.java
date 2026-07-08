package com.vn.sodu.user.service;

import com.vn.sodu.customer.Customer;
import com.vn.sodu.customer.dto.CreateCustomerRequest;
import com.vn.sodu.customer.service.CustomerService;
import com.vn.sodu.user.Account;
import com.vn.sodu.user.AccountRepo;
import com.vn.sodu.user.Role;
import com.vn.sodu.user.RoleRepo;
import com.vn.sodu.global.exception.BadRequestException;
import com.vn.sodu.global.exception.UnauthorizedException;
import com.vn.sodu.global.exception.ForbiddenOperationException;
import com.vn.sodu.global.exception.NotFoundException;
import com.vn.sodu.user.dto.*;
import com.vn.sodu.user.mapper.AccountMapper;
import com.vn.sodu.security.JwtService;
import com.vn.sodu.security.TokenBlacklistService;
import com.vn.sodu.utilites.PasswordEncrypt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AccountRepo accountRepo;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final AccountMapper accountMapper;
    private final PasswordEncrypt passwordEncrypt;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final CustomerService customerService;
    private final TokenBlacklistService tokenBlacklistService;
    private final RoleRepo roleRepo;

    /**
     * Authenticate user and generate JWT tokens
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        try {
            // Validate input
            if (request.getEmail() == null || request.getEmail().isEmpty()) {
                throw new BadRequestException("Email is required");
            }
            if (request.getPassword() == null || request.getPassword().isEmpty()) {
                throw new BadRequestException("Password is required");
            }

            // Find user by email
            Account account = accountRepo.findByEmail(request.getEmail())
                    .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

            // Check if account is banned
            ensureAccountIsNotBanned(account, request.getEmail());

            // Decrypt stored password and compare with request password
            String storedHash = account.getPasswordHash();
            boolean passwordMatches = false;

            // Check if it's already a BCrypt hash (starts with "$2a$" or "$2b$" etc. -
            // usually length 60)
            if (storedHash != null && (storedHash.startsWith("$2a$") || storedHash.startsWith("$2b$")
                    || storedHash.startsWith("$2y$"))) {
                passwordMatches = passwordEncoder.matches(request.getPassword(), storedHash);
            } else {
                // Legacy AES decryption
                try {
                    String decryptedPassword = passwordEncrypt.decrypt(storedHash);
                    if (Objects.equals(decryptedPassword, request.getPassword())) {
                        passwordMatches = true;
                        // Temporary migration path: accept legacy password once, then rehash to BCrypt
                        account.setPasswordHash(passwordEncoder.encode(request.getPassword()));
                        accountRepo.save(account);
                        log.info("Migrated password to BCrypt for email: {}", request.getEmail());
                    }
                } catch (Exception e) {
                    log.warn("Legacy password decryption failed for email: {}", request.getEmail());
                }
            }

            if (!passwordMatches) {
                log.warn("Incorrect password for email: {}", request.getEmail());
                throw new UnauthorizedException("Invalid email or password");
            }

            // Generate JWT tokens
            // Create UserDetails for JWT generation
            UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
            String accessToken = jwtService.generateAccessToken(userDetails);
            String refreshToken = jwtService.generateRefreshToken(userDetails);

            log.info("Login successful for email: {}", request.getEmail());

            return LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtService.getAccessTokenExpiresInSeconds())
                    .account(accountMapper.toDTO(account))
                    .build();

        } catch (RuntimeException e) {
            log.error("Login failed for email: {}", request.getEmail(), e);
            throw e;
        } catch (Exception e) {
            log.error("Login error for email: {}", request.getEmail(), e);
            throw new UnauthorizedException("Invalid email or password");
        }
    }

    /**
     * Refresh access token using refresh token
     */
    @Transactional(readOnly = true)
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        log.info("Refresh token request");

        try {
            final String refreshToken = request.getRefreshToken();

            // Validate refresh token
            if (!jwtService.isRefreshTokenValid(refreshToken)) {
                throw new UnauthorizedException("Invalid or expired refresh token");
            }

            // Extract username from refresh token
            final String email = jwtService.extractUsername(refreshToken);

            // Load user details
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            Account account = accountRepo.findByEmail(email)
                    .orElseThrow(() -> new NotFoundException("User not found"));
            ensureAccountIsNotBanned(account, email);

            if (tokenBlacklistService.isBlacklisted(refreshToken)) {
                throw new UnauthorizedException("Invalid or expired refresh token");
            }

            // Generate new access token
            String newAccessToken = jwtService.generateAccessToken(userDetails);

            log.info("Refresh token successful for email: {}", email);

            return LoginResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtService.getAccessTokenExpiresInSeconds())
                    .account(accountMapper.toDTO(account))
                    .build();

        } catch (RuntimeException e) {
            log.error("Refresh token failed", e);
            throw e;
        } catch (Exception e) {
            log.error("Refresh token failed", e);
            throw new UnauthorizedException("Failed to refresh token");
        }
    }

    /**
     * Register new user
     */
    public RegisterResponse register(RegisterRequest request) {
        log.info("Register attempt for email: {}", request.getEmail());

        // Check if email already exists
        if (accountRepo.findByEmail(request.getEmail()).isPresent()) {
            throw new BadRequestException("Email already registered");
        }
        try {
            Account account = accountMapper.toEntity(request);
            account.setPasswordHash(passwordEncoder.encode(request.getPassword()));

            // Set default role to 2 (Customer)
            account.setRole(getDefaultUserRole());

            Account savedAccount = accountRepo.save(account);

            customerService.createCustomer(savedAccount.getId(),
                    CreateCustomerRequest.builder().build());

            log.info("Registration successful for email: {}", request.getEmail());
            return accountMapper.toRegisterResponse(savedAccount);
        } catch (RuntimeException e) {
            log.error("Registration failed for email: {}", request.getEmail(), e);
            throw e;
        } catch (Exception e) {
            log.error("Registration failed for email: {}", request.getEmail(), e);
            throw new BadRequestException("Registration failed due to an internal error.");
        }
    }

    public void logout(String accessToken) {
        logout(accessToken, null);
    }

    public void logout(String accessToken, String refreshToken) {
        log.info("Logout request");

        blacklistToken(accessToken, "access");
        blacklistToken(refreshToken, "refresh");
        SecurityContextHolder.clearContext();
    }

    @Transactional
    public AccountDTO updateCurrentAccountPhone(UpdatePhoneRequest request) {
        String email = getAuthenticatedEmail();
        String phone = request != null ? request.getPhone() : null;
        String normalizedPhone = phone != null ? phone.trim() : "";

        if (normalizedPhone.isEmpty()) {
            throw new BadRequestException("Phone number is required");
        }

        Account account = accountRepo.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));

        accountRepo.findByPhone(normalizedPhone)
                .filter(existingAccount -> !existingAccount.getId().equals(account.getId()))
                .ifPresent(existingAccount -> {
                    throw new BadRequestException("Phone number is already in use");
                });

        if (!normalizedPhone.equals(account.getPhone())) {
            account.setPhone(normalizedPhone);
            account = accountRepo.save(account);
        }

        return accountMapper.toDTO(account);
    }

    private void ensureAccountIsNotBanned(Account account, String email) {
        if (account.getStatus() == Account.AccountStatus.BANNED) {
            log.warn("Auth attempt for banned account: {}", email);
            throw new UnauthorizedException("Account is banned.");
        }
    }

    private String getAuthenticatedEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Authentication is required");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }

        String name = authentication.getName();
        if (name == null || name.isBlank() || "anonymousUser".equals(name)) {
            throw new UnauthorizedException("Authentication is required");
        }

        return name;
    }

    private Role getDefaultUserRole() {
        return roleRepo.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("Default USER role is not configured"));
    }

    private void blacklistToken(String token, String expectedType) {
        if (token == null || token.isBlank()) {
            return;
        }

        try {
            if (!jwtService.isTokenValid(token)) {
                log.warn("Ignoring invalid {} token during logout", expectedType);
                return;
            }

            String actualType = jwtService.extractTokenType(token);
            if (!expectedType.equals(actualType)) {
                log.warn("Ignoring {} token presented as {} during logout", actualType, expectedType);
                return;
            }

            tokenBlacklistService.blacklist(token, jwtService.extractExpiration(token));
        } catch (Exception e) {
            log.warn("Failed to blacklist {} token during logout: {}", expectedType, e.getMessage());
        }
    }
}
