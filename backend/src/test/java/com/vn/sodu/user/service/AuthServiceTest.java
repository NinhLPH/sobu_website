package com.vn.sodu.user.service;

import com.vn.sodu.mail.EmailService;
import com.vn.sodu.user.Account;
import com.vn.sodu.user.AccountRepo;
import com.vn.sodu.user.ActivationToken;
import com.vn.sodu.user.ActivationTokenRepo;
import com.vn.sodu.user.Role;
import com.vn.sodu.user.dto.*;
import com.vn.sodu.user.mapper.AccountMapper;
import com.vn.sodu.security.JwtService;
import com.vn.sodu.utilites.PasswordEncrypt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Auth Service Tests")
class AuthServiceTest {

    @Mock
    private AccountRepo accountRepo;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private PasswordEncrypt passwordEncrypt;

    @Mock
    private ActivationTokenRepo activationTokenRepo;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private Account testAccount;
    private UserDetails testUserDetails;
    private LoginRequest loginRequest;
    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setUsername("test@example.com");
        testAccount.setPasswordHash("encryptedPassword");
        testAccount.setStatus(Account.AccountStatus.ACTIVE);

        testUserDetails = new User("test@example.com", "password", java.util.Collections.emptyList());

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        registerRequest = new RegisterRequest();
        registerRequest.setEmail("newuser@example.com");
        registerRequest.setPassword("newpassword123");
        registerRequest.setFullName("John Doe");
        registerRequest.setPhone("0123456789");
    }

    // ─── Login Tests ────────────────────────────────────────────────

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void testLoginSuccess() {
        when(accountRepo.findByUsername("test@example.com")).thenReturn(Optional.of(testAccount));
        when(passwordEncrypt.decrypt("encryptedPassword")).thenReturn("password123");
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(testUserDetails);
        when(jwtService.generateAccessToken(testUserDetails)).thenReturn("accessToken");
        when(jwtService.generateRefreshToken(testUserDetails)).thenReturn("refreshToken");

        AccountDTO accountDTO = new AccountDTO();
        when(accountMapper.toDTO(testAccount)).thenReturn(accountDTO);

        LoginResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("accessToken", response.getAccessToken());
        assertEquals("refreshToken", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(3600L, response.getExpiresIn());
        verify(accountRepo).findByUsername("test@example.com");
    }

    @Test
    @DisplayName("Should fail login with non-existent user")
    void testLoginUserNotFound() {
        when(accountRepo.findByUsername("nonexistent@example.com")).thenReturn(Optional.empty());

        loginRequest.setEmail("nonexistent@example.com");
        
        assertThrows(RuntimeException.class, () -> authService.login(loginRequest));
        verify(accountRepo).findByUsername("nonexistent@example.com");
    }

    @Test
    @DisplayName("Should fail login with incorrect password")
    void testLoginInvalidPassword() {
        when(accountRepo.findByUsername("test@example.com")).thenReturn(Optional.of(testAccount));
        when(passwordEncrypt.decrypt("encryptedPassword")).thenReturn("correctPassword");

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.login(loginRequest));
        assertEquals("Invalid email or password", exception.getMessage());
    }

    @Test
    @DisplayName("Should fail login with inactive account")
    void testLoginInactiveAccount() {
        testAccount.setStatus(Account.AccountStatus.INACTIVE);
        when(accountRepo.findByUsername("test@example.com")).thenReturn(Optional.of(testAccount));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.login(loginRequest));
        assertEquals("Account is not active. Please activate your account.", exception.getMessage());
    }

    @Test
    @DisplayName("Should fail login with banned account")
    void testLoginBannedAccount() {
        testAccount.setStatus(Account.AccountStatus.BANNED);
        when(accountRepo.findByUsername("test@example.com")).thenReturn(Optional.of(testAccount));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.login(loginRequest));
        assertEquals("Account is not active. Please activate your account.", exception.getMessage());
    }

    @Test
    @DisplayName("Should fail login with empty email")
    void testLoginEmptyEmail() {
        loginRequest.setEmail("");

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.login(loginRequest));
        assertEquals("Email is required", exception.getMessage());
    }

    @Test
    @DisplayName("Should fail login with null email")
    void testLoginNullEmail() {
        loginRequest.setEmail(null);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.login(loginRequest));
        assertEquals("Email is required", exception.getMessage());
    }

    @Test
    @DisplayName("Should fail login with empty password")
    void testLoginEmptyPassword() {
        loginRequest.setPassword("");

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.login(loginRequest));
        assertEquals("Password is required", exception.getMessage());
    }

    @Test
    @DisplayName("Should fail login with null password")
    void testLoginNullPassword() {
        loginRequest.setPassword(null);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.login(loginRequest));
        assertEquals("Password is required", exception.getMessage());
    }

    // ─── Refresh Token Tests ────────────────────────────────────────────────

    @Test
    @DisplayName("Should refresh token successfully")
    void testRefreshTokenSuccess() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("validRefreshToken");

        when(jwtService.isTokenValid("validRefreshToken")).thenReturn(true);
        when(jwtService.extractUsername("validRefreshToken")).thenReturn("test@example.com");
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(testUserDetails);
        when(accountRepo.findByUsername("test@example.com")).thenReturn(Optional.of(testAccount));
        when(jwtService.generateAccessToken(testUserDetails)).thenReturn("newAccessToken");

        AccountDTO accountDTO = new AccountDTO();
        when(accountMapper.toDTO(testAccount)).thenReturn(accountDTO);

        LoginResponse response = authService.refreshToken(request);

        assertNotNull(response);
        assertEquals("newAccessToken", response.getAccessToken());
        assertEquals("validRefreshToken", response.getRefreshToken());
        verify(jwtService).isTokenValid("validRefreshToken");
    }

    @Test
    @DisplayName("Should fail refresh token with invalid token")
    void testRefreshTokenInvalid() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("invalidToken");

        when(jwtService.isTokenValid("invalidToken")).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.refreshToken(request));
        assertEquals("Invalid or expired refresh token", exception.getMessage());
    }

    @Test
    @DisplayName("Should fail refresh token with expired token")
    void testRefreshTokenExpired() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("expiredToken");

        when(jwtService.isTokenValid("expiredToken")).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.refreshToken(request));
        assertEquals("Invalid or expired refresh token", exception.getMessage());
    }

    @Test
    @DisplayName("Should fail refresh token when user not found")
    void testRefreshTokenUserNotFound() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("validToken");

        when(jwtService.isTokenValid("validToken")).thenReturn(true);
        when(jwtService.extractUsername("validToken")).thenReturn("nonexistent@example.com");
        when(userDetailsService.loadUserByUsername("nonexistent@example.com")).thenReturn(testUserDetails);
        when(accountRepo.findByUsername("nonexistent@example.com")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.refreshToken(request));
        assertEquals("User not found", exception.getMessage());
    }

    // ─── Register Tests ────────────────────────────────────────────────

    @Test
    @DisplayName("Should register new user successfully")
    void testRegisterSuccess() {
        Account newAccount = new Account();
        RegisterResponse registerResponse = new RegisterResponse();
        
        when(accountRepo.findByUsername("newuser@example.com")).thenReturn(Optional.empty());
        when(passwordEncrypt.encrypt("newpassword123")).thenReturn("encryptedNewPassword");
        when(accountMapper.toEntity(registerRequest)).thenReturn(newAccount);
        when(accountRepo.save(any(Account.class))).thenReturn(newAccount);
        when(activationTokenRepo.save(any(ActivationToken.class))).thenReturn(new ActivationToken());
        when(accountMapper.toRegisterResponse(newAccount)).thenReturn(registerResponse);

        RegisterResponse response = authService.register(registerRequest);

        assertNotNull(response);
        verify(accountRepo).findByUsername("newuser@example.com");
        verify(passwordEncrypt).encrypt("newpassword123");
        verify(accountRepo).save(any(Account.class));
        verify(emailService).sendActivationEmail(any(Account.class), anyString());
    }

    @Test
    @DisplayName("Should fail register when email already exists")
    void testRegisterEmailAlreadyExists() {
        when(accountRepo.findByUsername("newuser@example.com")).thenReturn(Optional.of(testAccount));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.register(registerRequest));
        assertEquals("Email already registered", exception.getMessage());
    }

    @Test
    @DisplayName("Should set account to inactive status on registration")
    void testRegisterAccountInactiveStatus() {
        when(accountRepo.findByUsername("newuser@example.com")).thenReturn(Optional.empty());
        Account newAccount = new Account();
        when(accountMapper.toEntity(registerRequest)).thenReturn(newAccount);
        when(passwordEncrypt.encrypt("newpassword123")).thenReturn("encrypted");
        when(accountRepo.save(any(Account.class))).thenReturn(newAccount);
        when(activationTokenRepo.save(any(ActivationToken.class))).thenReturn(new ActivationToken());

        authService.register(registerRequest);

        assertEquals(Account.AccountStatus.INACTIVE, newAccount.getStatus());
    }

    @Test
    @DisplayName("Should set customer role on registration")
    void testRegisterCustomerRoleAssigned() {
        when(accountRepo.findByUsername("newuser@example.com")).thenReturn(Optional.empty());
        Account newAccount = new Account();
        when(accountMapper.toEntity(registerRequest)).thenReturn(newAccount);
        when(passwordEncrypt.encrypt("newpassword123")).thenReturn("encrypted");
        when(accountRepo.save(any(Account.class))).thenReturn(newAccount);
        when(activationTokenRepo.save(any(ActivationToken.class))).thenReturn(new ActivationToken());

        authService.register(registerRequest);

        assertNotNull(newAccount.getRole());
        assertEquals(2, newAccount.getRole().getId());
    }

    // ─── Account Activation Tests ────────────────────────────────────────────────

    @Test
    @DisplayName("Should activate account successfully")
    void testActivateAccountSuccess() {
        ActivationToken activationToken = new ActivationToken();
        activationToken.setAccount(testAccount);
        activationToken.setExpiresAt(LocalDateTime.now().plusHours(1));

        RegisterResponse registerResponse = new RegisterResponse();
        
        when(activationTokenRepo.findByToken("validToken")).thenReturn(Optional.of(activationToken));
        when(accountRepo.save(testAccount)).thenReturn(testAccount);
        when(accountMapper.toRegisterResponse(testAccount)).thenReturn(registerResponse);

        RegisterResponse response = authService.activateAccount("validToken");

        assertNotNull(response);
        assertEquals(Account.AccountStatus.ACTIVE, testAccount.getStatus());
        verify(activationTokenRepo).delete(activationToken);
    }

    @Test
    @DisplayName("Should fail activate account with invalid token")
    void testActivateAccountInvalidToken() {
        when(activationTokenRepo.findByToken("invalidToken")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.activateAccount("invalidToken"));
        assertEquals("Invalid or expired activation token", exception.getMessage());
    }

    @Test
    @DisplayName("Should fail activate account with expired token")
    void testActivateAccountExpiredToken() {
        ActivationToken activationToken = new ActivationToken();
        activationToken.setAccount(testAccount);
        activationToken.setExpiresAt(LocalDateTime.now().minusHours(1));

        when(activationTokenRepo.findByToken("expiredToken")).thenReturn(Optional.of(activationToken));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.activateAccount("expiredToken"));
        assertEquals("Activation token expired", exception.getMessage());
    }

    // ─── Logout Tests ────────────────────────────────────────────────

    @Test
    @DisplayName("Should handle logout without error")
    void testLogoutSuccess() {
        assertDoesNotThrow(() -> authService.logout("validToken"));
    }

    @Test
    @DisplayName("Should handle logout with any token")
    void testLogoutWithAnyToken() {
        assertDoesNotThrow(() -> authService.logout("anyToken"));
        assertDoesNotThrow(() -> authService.logout(""));
        assertDoesNotThrow(() -> authService.logout(null));
    }
}
