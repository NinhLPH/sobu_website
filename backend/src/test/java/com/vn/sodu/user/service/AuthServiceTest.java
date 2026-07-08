package com.vn.sodu.user.service;

import com.vn.sodu.customer.service.CustomerService;
import com.vn.sodu.user.Account;
import com.vn.sodu.user.AccountRepo;
import com.vn.sodu.user.Role;
import com.vn.sodu.user.RoleRepo;
import com.vn.sodu.security.TokenBlacklistService;
import com.vn.sodu.user.dto.*;
import com.vn.sodu.user.mapper.AccountMapper;
import com.vn.sodu.security.JwtService;
import com.vn.sodu.utilites.PasswordEncrypt;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Mock
    private com.vn.sodu.customer.service.CustomerService customerService;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private RoleRepo roleRepo;

    @InjectMocks
    private AuthService authService;

    private Account testAccount;
    private Role userRole;
    private UserDetails testUserDetails;
    private LoginRequest loginRequest;
    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setEmail("test@example.com");
        testAccount.setPasswordHash("encryptedPassword");
        testAccount.setStatus(Account.AccountStatus.ACTIVE);
        testAccount.setFullName("Test User");
        testAccount.setPhone("0123456789");

        userRole = Role.builder()
                .id(2)
                .name("USER")
                .build();

        testUserDetails = new User("test@example.com", "password", java.util.Collections.emptyList());

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        registerRequest = new RegisterRequest();
        registerRequest.setEmail("newuser@example.com");
        registerRequest.setPassword("newpassword123");
        registerRequest.setFullName("John Doe");
        registerRequest.setPhone("0123456789");

        lenient().when(roleRepo.findByName("USER")).thenReturn(Optional.of(userRole));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String email) {
        UserDetails userDetails = new User(email, "password", java.util.Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );
    }

    // ─── Login Tests ────────────────────────────────────────────────

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void testLoginSuccess() {
        when(accountRepo.findByEmail("test@example.com")).thenReturn(Optional.of(testAccount));
        when(passwordEncrypt.decrypt("encryptedPassword")).thenReturn("password123");
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashed");
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(testUserDetails);
        when(jwtService.generateAccessToken(testUserDetails)).thenReturn("accessToken");
        when(jwtService.generateRefreshToken(testUserDetails)).thenReturn("refreshToken");
        when(jwtService.getAccessTokenExpiresInSeconds()).thenReturn(3600L);

        AccountDTO accountDTO = new AccountDTO();
        when(accountMapper.toDTO(testAccount)).thenReturn(accountDTO);

        LoginResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("accessToken", response.getAccessToken());
        assertEquals("refreshToken", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(3600L, response.getExpiresIn());
        verify(accountRepo).findByEmail("test@example.com");
    }

    @Test
    @DisplayName("Should fail login with non-existent user")
    void testLoginUserNotFound() {
        when(accountRepo.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        loginRequest.setEmail("nonexistent@example.com");
        
        assertThrows(RuntimeException.class, () -> authService.login(loginRequest));
        verify(accountRepo).findByEmail("nonexistent@example.com");
    }

    @Test
    @DisplayName("Should fail login with incorrect password")
    void testLoginInvalidPassword() {
        when(accountRepo.findByEmail("test@example.com")).thenReturn(Optional.of(testAccount));
        when(passwordEncrypt.decrypt("encryptedPassword")).thenReturn("correctPassword");

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.login(loginRequest));
        assertEquals("Invalid email or password", exception.getMessage());
    }

    @Test
    @DisplayName("Should fail login with banned account")
    void testLoginBannedAccount() {
        testAccount.setStatus(Account.AccountStatus.BANNED);
        when(accountRepo.findByEmail("test@example.com")).thenReturn(Optional.of(testAccount));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.login(loginRequest));
        assertEquals("Account is banned.", exception.getMessage());
    }

    @Test
    @DisplayName("Should login successfully with inactive (non-banned) account")
    void testLoginInactiveAccountAllowed() {
        testAccount.setStatus(Account.AccountStatus.INACTIVE);
        when(accountRepo.findByEmail("test@example.com")).thenReturn(Optional.of(testAccount));
        when(passwordEncrypt.decrypt("encryptedPassword")).thenReturn("password123");
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashed");
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(testUserDetails);
        when(jwtService.generateAccessToken(testUserDetails)).thenReturn("accessToken");
        when(jwtService.generateRefreshToken(testUserDetails)).thenReturn("refreshToken");
        when(jwtService.getAccessTokenExpiresInSeconds()).thenReturn(3600L);

        AccountDTO accountDTO = new AccountDTO();
        when(accountMapper.toDTO(testAccount)).thenReturn(accountDTO);

        LoginResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("accessToken", response.getAccessToken());
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

        when(jwtService.isRefreshTokenValid("validRefreshToken")).thenReturn(true);
        when(jwtService.extractUsername("validRefreshToken")).thenReturn("test@example.com");
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(testUserDetails);
        when(accountRepo.findByEmail("test@example.com")).thenReturn(Optional.of(testAccount));
        when(jwtService.generateAccessToken(testUserDetails)).thenReturn("newAccessToken");
        when(jwtService.getAccessTokenExpiresInSeconds()).thenReturn(3600L);

        AccountDTO accountDTO = new AccountDTO();
        when(accountMapper.toDTO(testAccount)).thenReturn(accountDTO);

        LoginResponse response = authService.refreshToken(request);

        assertNotNull(response);
        assertEquals("newAccessToken", response.getAccessToken());
        assertEquals("validRefreshToken", response.getRefreshToken());
        assertEquals(3600L, response.getExpiresIn());
        verify(jwtService).isRefreshTokenValid("validRefreshToken");
    }

    @Test
    @DisplayName("Should fail refresh token when account is banned")
    void testRefreshTokenBannedAccount() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("validRefreshToken");
        testAccount.setStatus(Account.AccountStatus.BANNED);

        when(jwtService.isRefreshTokenValid("validRefreshToken")).thenReturn(true);
        when(jwtService.extractUsername("validRefreshToken")).thenReturn("test@example.com");
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(testUserDetails);
        when(accountRepo.findByEmail("test@example.com")).thenReturn(Optional.of(testAccount));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.refreshToken(request));
        assertEquals("Account is banned.", exception.getMessage());
    }

    @Test
    @DisplayName("Should fail refresh token with invalid token")
    void testRefreshTokenInvalid() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("invalidToken");

        when(jwtService.isRefreshTokenValid("invalidToken")).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.refreshToken(request));
        assertEquals("Invalid or expired refresh token", exception.getMessage());
    }

    @Test
    @DisplayName("Should fail refresh token with expired token")
    void testRefreshTokenExpired() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("expiredToken");

        when(jwtService.isRefreshTokenValid("expiredToken")).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.refreshToken(request));
        assertEquals("Invalid or expired refresh token", exception.getMessage());
    }

    @Test
    @DisplayName("Should fail refresh token when user not found")
    void testRefreshTokenUserNotFound() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("validToken");

        when(jwtService.isRefreshTokenValid("validToken")).thenReturn(true);
        when(jwtService.extractUsername("validToken")).thenReturn("nonexistent@example.com");
        when(userDetailsService.loadUserByUsername("nonexistent@example.com")).thenReturn(testUserDetails);
        when(accountRepo.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.refreshToken(request));
        assertEquals("User not found", exception.getMessage());
    }

    // ─── Register Tests ────────────────────────────────────────────────

    @Test
    @DisplayName("Should register new user successfully")
    void testRegisterSuccess() {
        Account newAccount = new Account();
        RegisterResponse registerResponse = new RegisterResponse();

        when(accountRepo.findByEmail("newuser@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("newpassword123")).thenReturn("encryptedNewPassword");
        when(accountMapper.toEntity(registerRequest)).thenReturn(newAccount);
        when(accountRepo.save(any(Account.class))).thenReturn(newAccount);
        when(customerService.createCustomer(any(), any())).thenReturn(null);
        when(accountMapper.toRegisterResponse(newAccount)).thenReturn(registerResponse);

        RegisterResponse response = authService.register(registerRequest);

        assertNotNull(response);
        verify(accountRepo).findByEmail("newuser@example.com");
        verify(passwordEncoder).encode("newpassword123");
        verify(accountRepo).save(any(Account.class));
    }

    @Test
    @DisplayName("Should fail register when email already exists")
    void testRegisterEmailAlreadyExists() {
        when(accountRepo.findByEmail("newuser@example.com")).thenReturn(Optional.of(testAccount));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.register(registerRequest));
        assertEquals("Email already registered", exception.getMessage());
    }

    @Test
    @DisplayName("Should set account to active status on registration")
    void testRegisterAccountActiveStatus() {
        when(accountRepo.findByEmail("newuser@example.com")).thenReturn(Optional.empty());
        Account newAccount = new Account();
        newAccount.setStatus(Account.AccountStatus.ACTIVE);
        when(accountMapper.toEntity(registerRequest)).thenReturn(newAccount);
        when(passwordEncoder.encode("newpassword123")).thenReturn("encrypted");
        when(accountRepo.save(any(Account.class))).thenReturn(newAccount);

        authService.register(registerRequest);

        assertEquals(Account.AccountStatus.ACTIVE, newAccount.getStatus());
    }

    @Test
    @DisplayName("Should set customer role on registration")
    void testRegisterCustomerRoleAssigned() {
        when(accountRepo.findByEmail("newuser@example.com")).thenReturn(Optional.empty());
        Account newAccount = new Account();
        when(accountMapper.toEntity(registerRequest)).thenReturn(newAccount);
        when(passwordEncoder.encode("newpassword123")).thenReturn("encrypted");
        when(accountRepo.save(any(Account.class))).thenReturn(newAccount);

        authService.register(registerRequest);

        assertNotNull(newAccount.getRole());
        assertEquals(2, newAccount.getRole().getId());
    }

    // ─── Logout Tests ────────────────────────────────────────────────

    @Test
    @DisplayName("Should handle logout without error")
    void testLogoutSuccess() {
        java.util.Date accessExpiry = java.util.Date.from(LocalDateTime.now().plusMinutes(5)
                .atZone(java.time.ZoneId.systemDefault()).toInstant());
        java.util.Date refreshExpiry = java.util.Date.from(LocalDateTime.now().plusMinutes(10)
                .atZone(java.time.ZoneId.systemDefault()).toInstant());

        when(jwtService.isTokenValid("validAccessToken")).thenReturn(true);
        when(jwtService.extractTokenType("validAccessToken")).thenReturn("access");
        when(jwtService.extractExpiration("validAccessToken")).thenReturn(accessExpiry);
        when(jwtService.isTokenValid("validRefreshToken")).thenReturn(true);
        when(jwtService.extractTokenType("validRefreshToken")).thenReturn("refresh");
        when(jwtService.extractExpiration("validRefreshToken")).thenReturn(refreshExpiry);

        assertDoesNotThrow(() -> authService.logout("validAccessToken", "validRefreshToken"));
        verify(tokenBlacklistService).blacklist("validAccessToken", accessExpiry);
        verify(tokenBlacklistService).blacklist("validRefreshToken", refreshExpiry);
    }

    @Test
    @DisplayName("Should handle logout with any token")
    void testLogoutWithAnyToken() {
        when(jwtService.isTokenValid("anyToken")).thenReturn(false);

        assertDoesNotThrow(() -> authService.logout("anyToken"));
        assertDoesNotThrow(() -> authService.logout(""));
        assertDoesNotThrow(() -> authService.logout(null));
        verify(tokenBlacklistService, never()).blacklist(eq("anyToken"), any(java.util.Date.class));
    }

    @Test
    @DisplayName("Should update current account phone successfully")
    void testUpdateCurrentAccountPhoneSuccess() {
        authenticateAs("test@example.com");
        UpdatePhoneRequest request = new UpdatePhoneRequest();
        request.setPhone(" 0987654321 ");
        AccountDTO accountDTO = new AccountDTO();
        accountDTO.setPhone("0987654321");

        when(accountRepo.findByEmail("test@example.com")).thenReturn(Optional.of(testAccount));
        when(accountRepo.findByPhone("0987654321")).thenReturn(Optional.empty());
        when(accountRepo.save(testAccount)).thenReturn(testAccount);
        when(accountMapper.toDTO(testAccount)).thenReturn(accountDTO);

        AccountDTO response = authService.updateCurrentAccountPhone(request);

        assertEquals("0987654321", testAccount.getPhone());
        assertEquals("0987654321", response.getPhone());
        verify(accountRepo).save(testAccount);
    }

    @Test
    @DisplayName("Should reject duplicate current account phone")
    void testUpdateCurrentAccountPhoneDuplicate() {
        authenticateAs("test@example.com");
        UpdatePhoneRequest request = new UpdatePhoneRequest();
        request.setPhone("0987654321");
        Account otherAccount = new Account();
        otherAccount.setId(2L);
        otherAccount.setPhone("0987654321");

        when(accountRepo.findByEmail("test@example.com")).thenReturn(Optional.of(testAccount));
        when(accountRepo.findByPhone("0987654321")).thenReturn(Optional.of(otherAccount));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.updateCurrentAccountPhone(request));

        assertEquals("Phone number is already in use", exception.getMessage());
        verify(accountRepo, never()).save(any(Account.class));
    }

    @Test
    @DisplayName("Should reject blank current account phone")
    void testUpdateCurrentAccountPhoneBlank() {
        authenticateAs("test@example.com");
        UpdatePhoneRequest request = new UpdatePhoneRequest();
        request.setPhone("   ");

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.updateCurrentAccountPhone(request));

        assertEquals("Phone number is required", exception.getMessage());
        verify(accountRepo, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("Should reject current account phone update when user is missing")
    void testUpdateCurrentAccountPhoneUserNotFound() {
        authenticateAs("missing@example.com");
        UpdatePhoneRequest request = new UpdatePhoneRequest();
        request.setPhone("0987654321");

        when(accountRepo.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.updateCurrentAccountPhone(request));

        assertEquals("User not found", exception.getMessage());
        verify(accountRepo, never()).save(any(Account.class));
    }
}
