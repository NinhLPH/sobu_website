package com.vn.sodu.user.service;

import com.vn.sodu.customer.Customer;
import com.vn.sodu.customer.dto.CreateCustomerRequest;
import com.vn.sodu.customer.service.CustomerService;
import com.vn.sodu.mail.EmailService;
import com.vn.sodu.user.*;
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
    private final ActivationTokenRepo activationTokenRepo;
    private final EmailService emailService;
    private final CustomerService customerService;

    /**
     * Authenticate user and generate JWT tokens
     */
    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());
        
        try {
            // Validate input
            if (request.getEmail() == null || request.getEmail().isEmpty()) {
                throw new RuntimeException("Email is required");
            }
            if (request.getPassword() == null || request.getPassword().isEmpty()) {
                throw new RuntimeException("Password is required");
            }

            // Find user by email
            Account account = accountRepo.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Check if account is active
            if (account.getStatus() != Account.AccountStatus.ACTIVE) {
                log.warn("Login attempt for inactive/banned account: {}", request.getEmail());
                throw new RuntimeException("Account is not active. Please activate your account.");
            }

            // Decrypt stored password and compare with request password
            String decryptedPassword = passwordEncrypt.decrypt(account.getPasswordHash());
            if (!Objects.equals(decryptedPassword, request.getPassword())) {
                log.warn("Incorrect password for email: {}", request.getEmail());
                throw new RuntimeException("Invalid email or password");
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
            throw new RuntimeException("Invalid email or password");
        }
    }

    /**
     * Refresh access token using refresh token
     */
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        log.info("Refresh token request");
        
        try {
            final String refreshToken = request.getRefreshToken();
            
            // Validate refresh token
            if (!jwtService.isTokenValid(refreshToken)) {
                throw new RuntimeException("Invalid or expired refresh token");
            }

            // Extract username from refresh token
            final String username = jwtService.extractUsername(refreshToken);
            
            // Load user details
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            Account account = accountRepo.findByEmail(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Generate new access token
            String newAccessToken = jwtService.generateAccessToken(userDetails);

            log.info("Refresh token successful for email: {}", username);

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
            throw new RuntimeException("Failed to refresh token");
        }
    }

    /**
     * Register new user
     */
    public RegisterResponse register(RegisterRequest request) {
        log.info("Register attempt for email: {}", request.getEmail());
        
        // Check if email already exists
        if (accountRepo.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }
       try {
            Account account = accountMapper.toEntity(request);
            account.setPasswordHash(passwordEncrypt.encrypt(request.getPassword()));
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
       } catch (Exception e) {
            log.error("Registration failed for email: {}", request.getEmail(), e);
            throw new RuntimeException("Registration failed: " + e.getMessage());
        }
    }

    /**
     * Logout - typically client-side only, but can be used to invalidate tokens
     */
    public RegisterResponse activateAccount(String token) {
        log.info("Activate account with token: {}", token);
        ActivationToken activationToken = activationTokenRepo.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired activation token"));
        if (activationToken.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            throw new RuntimeException("Activation token expired");
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

