package com.vn.sodu.user.service;

import com.vn.sodu.customer.dto.CreateCustomerRequest;
import com.vn.sodu.customer.service.CustomerService;
import com.vn.sodu.security.JwtService;
import com.vn.sodu.user.Account;
import com.vn.sodu.user.AccountRepo;
import com.vn.sodu.user.OAuthAccount;
import com.vn.sodu.user.OAuthAccountRepo;
import com.vn.sodu.user.Role;
import com.vn.sodu.user.RoleRepo;
import com.vn.sodu.user.mapper.AccountMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuth Service Tests")
class OAuthServiceTest {

    @Mock
    private AccountRepo accountRepo;

    @Mock
    private OAuthAccountRepo oAuthAccountRepo;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CustomerService customerService;

    @Mock
    private RoleRepo roleRepo;

    @InjectMocks
    private OAuthService oAuthService;

    @Test
    @DisplayName("Should create Google account with managed USER role")
    void createNewAccountUsesManagedUserRole() throws Exception {
        Role userRole = new Role();
        userRole.setId(2);
        userRole.setName("USER");

        when(roleRepo.findByName("USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(accountRepo.save(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            account.setId(42L);
            return account;
        });
        when(oAuthAccountRepo.save(any(OAuthAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account savedAccount = invokeCreateNewAccount("google-id", "user@example.com", "Google User", "avatar-url");

        assertSame(userRole, savedAccount.getRole());
        assertEquals("USER", savedAccount.getRole().getName());

        ArgumentCaptor<OAuthAccount> oauthCaptor = ArgumentCaptor.forClass(OAuthAccount.class);
        verify(oAuthAccountRepo).save(oauthCaptor.capture());
        assertSame(savedAccount, oauthCaptor.getValue().getAccount());
        assertEquals("google", oauthCaptor.getValue().getProvider());
        assertEquals("google-id", oauthCaptor.getValue().getProviderId());
        verify(customerService).createCustomer(eq(42L), any(CreateCustomerRequest.class));
    }

    @Test
    @DisplayName("Should fail with clear error when USER role is missing")
    void createNewAccountFailsWhenDefaultRoleMissing() throws Exception {
        when(roleRepo.findByName("USER")).thenReturn(Optional.empty());

        InvocationTargetException exception = assertThrows(InvocationTargetException.class,
                () -> invokeCreateNewAccount("google-id", "user@example.com", "Google User", "avatar-url"));

        assertEquals(IllegalStateException.class, exception.getCause().getClass());
        assertEquals("Default USER role is not configured", exception.getCause().getMessage());
    }

    private Account invokeCreateNewAccount(String googleId, String email, String name, String picture) throws Exception {
        Method method = OAuthService.class.getDeclaredMethod(
                "createNewAccount",
                String.class,
                String.class,
                String.class,
                String.class
        );
        method.setAccessible(true);
        return (Account) method.invoke(oAuthService, googleId, email, name, picture);
    }
}
