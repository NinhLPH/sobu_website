package com.vn.sodu.user.service;

import com.vn.sodu.customer.Customer;
import com.vn.sodu.customer.dto.CreateCustomerRequest;
import com.vn.sodu.customer.service.CustomerService;
import com.vn.sodu.mail.EmailService;
import com.vn.sodu.user.*;
import com.vn.sodu.global.exception.BadRequestException;
import com.vn.sodu.global.exception.UnauthorizedException;
import com.vn.sodu.global.exception.ForbiddenOperationException;
import com.vn.sodu.global.exception.NotFoundException;
import com.vn.sodu.user.dto.*;
import com.vn.sodu.user.mapper.AccountMapper;
import com.vn.sodu.security.JwtService;
import com.vn.sodu.utilites.PasswordEncrypt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
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
    private final ActivationTokenRepo activationTokenRepo;
    private final EmailService emailService;
    private final CustomerService customerService;

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

            // Check if account is active
            if (account.getStatus() != Account.AccountStatus.ACTIVE) {
                log.warn("Login attempt for non-active account: {}", request.getEmail());
                throw new UnauthorizedException("Account is not active. Please activate your account.");
            }

            // Decrypt stored password and compare with request password
            String storedHash = account.getPasswordHash();
            boolean passwordMatches = false;

            // Check if it's already a BCrypt hash (starts with "$2a$" or "$2b$" etc. - usually length 60)
            if (storedHash != null && (storedHash.startsWith("$2a$") || storedHash.startsWith("$2b$") || storedHash.startsWith("$2y$"))) {
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
                    .expiresIn(3600L) // 1 hour in seconds
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
            if (!jwtService.isTokenValid(refreshToken)) {
                throw new UnauthorizedException("Invalid or expired refresh token");
            }

            // Extract username from refresh token
            final String email = jwtService.extractUsername(refreshToken);
            
            // Load user details
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            Account account = accountRepo.findByEmail(email)
                    .orElseThrow(() -> new NotFoundException("User not found"));

            // Generate new access token
            String newAccessToken = jwtService.generateAccessToken(userDetails);

            log.info("Refresh token successful for email: {}", email);

            return LoginResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(3600L) // 1 hour in seconds
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
            account.setStatus(Account.AccountStatus.INACTIVE);

             
             // Set default role to 2 (Customer)
             Role customerRole = new Role();
             customerRole.setId(2);
             account.setRole(customerRole);
            
            
            Account savedAccount = accountRepo.save(account);

            Customer newCustomer = customerService.createCustomer(savedAccount.getId(),
                    CreateCustomerRequest.builder().build() );
           // create activation token
            String token = java.util.UUID.randomUUID().toString();
            ActivationToken activationToken = ActivationToken.builder()
                    .token(token)
                    .account(savedAccount)
                    .expiresAt(java.time.LocalDateTime.now().plusHours(24))
                    .build();
            activationTokenRepo.save(activationToken);
           // send activation email
            emailService.sendActivationEmail(savedAccount, token);
            
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

    /**
     * Logout - typically client-side only, but can be used to invalidate tokens
     */
    public RegisterResponse activateAccount(String token) {
        log.info("Activate account with token: {}", token);
        ActivationToken activationToken = activationTokenRepo.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid or expired activation token"));
        if (activationToken.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            throw new BadRequestException("Activation token expired");
        }
        Account account = activationToken.getAccount();
        account.setStatus(Account.AccountStatus.ACTIVE);
        Account saved = accountRepo.save(account);
        activationTokenRepo.delete(activationToken);
        return accountMapper.toRegisterResponse(saved);
    }

    public void logout(String token) {
        log.info("Logout request");
        // In a production system, you might want to:
        // 1. Add token to a blacklist
        // 2. Update user's last logout timestamp
        // For now, just log
    }
}

