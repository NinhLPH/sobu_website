package com.vn.sodu.user.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.vn.sodu.customer.dto.CreateCustomerRequest;
import com.vn.sodu.customer.service.CustomerService;
import com.vn.sodu.global.exception.BadRequestException;
import com.vn.sodu.global.exception.UnauthorizedException;
import com.vn.sodu.security.JwtService;
import com.vn.sodu.user.*;
import com.vn.sodu.user.dto.GoogleLoginRequest;
import com.vn.sodu.user.dto.LoginResponse;
import com.vn.sodu.user.mapper.AccountMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuthService {

    private final GoogleProperties googleProperties;

    private final AccountRepo accountRepo;
    private final OAuthAccountRepo oAuthAccountRepo;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final AccountMapper accountMapper;
    private final PasswordEncoder passwordEncoder;
    private final CustomerService customerService;
    private final RoleRepo roleRepo;

    private GoogleIdTokenVerifier verifier;

    @PostConstruct
    public void init() {
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(googleProperties.getClientId()))
                .build();
    }

    @Transactional
    public LoginResponse loginWithGoogle(GoogleLoginRequest request) {
        if (request.getIdToken() == null || request.getIdToken().isBlank()) {
            throw new BadRequestException("Google ID token is required");
        }

        GoogleIdToken.Payload payload = verifyGoogleToken(request.getIdToken());
        String googleId = payload.getSubject();
        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String picture = (String) payload.get("picture");

        if (email == null || email.isBlank()) {
            throw new BadRequestException("Google account must have an email address");
        }

        Account account = findOrCreateAccount(googleId, email, name, picture);

        if (account.getStatus() == Account.AccountStatus.BANNED) {
            throw new UnauthorizedException("Account is banned.");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(account.getEmail());
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        log.info("Google login successful for email: {}", email);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpiresInSeconds())
                .account(accountMapper.toDTO(account))
                .build();
    }

    private GoogleIdToken.Payload verifyGoogleToken(String idToken) {
        try {
            GoogleIdToken token = verifier.verify(idToken);
            if (token == null) {
                throw new UnauthorizedException("Invalid Google ID token");
            }
            return token.getPayload();
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Google token verification failed", e);
            throw new UnauthorizedException("Google token verification failed");
        }
    }

    private Account findOrCreateAccount(String googleId, String email, String name, String picture) {
        return oAuthAccountRepo.findByProviderAndProviderId("google", googleId)
                .map(OAuthAccount::getAccount)
                .orElseGet(() -> accountRepo.findByEmail(email)
                        .map(account -> linkOAuthAccount(account, googleId, email, name, picture))
                        .orElseGet(() -> createNewAccount(googleId, email, name, picture)));
    }

    private Account linkOAuthAccount(Account account, String googleId, String email, String name, String picture) {
        OAuthAccount oAuthAccount = OAuthAccount.builder()
                .account(account)
                .provider("google")
                .providerId(googleId)
                .email(email)
                .name(name)
                .avatarUrl(picture)
                .build();
        oAuthAccountRepo.save(oAuthAccount);
        log.info("Linked Google account {} to existing account {}", googleId, account.getEmail());
        return account;
    }

    private Account createNewAccount(String googleId, String email, String name, String picture) {
        Account account = Account.builder()
                .email(email)
                .fullName(name != null ? name : email)
                .passwordHash(passwordEncoder.encode(java.util.UUID.randomUUID().toString()))
                .role(getDefaultUserRole())
                .status(Account.AccountStatus.ACTIVE)
                .build();

        Account savedAccount = accountRepo.save(account);

        OAuthAccount oAuthAccount = OAuthAccount.builder()
                .account(savedAccount)
                .provider("google")
                .providerId(googleId)
                .email(email)
                .name(name)
                .avatarUrl(picture)
                .build();
        oAuthAccountRepo.save(oAuthAccount);

        customerService.createCustomer(savedAccount.getId(), CreateCustomerRequest.builder().build());

        log.info("Created new account for Google user {} with email {}", googleId, email);
        return savedAccount;
    }

    private Role getDefaultUserRole() {
        return roleRepo.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("Default USER role is not configured"));
    }
}
