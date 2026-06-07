package com.vn.sodu.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        log.debug("Processing request to '{}' with method '{}'", request.getRequestURI(), request.getMethod());

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No Bearer token found in request to '{}'", request.getRequestURI());
            chain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(7);
        if (tokenBlacklistService.isBlacklisted(jwt)) {
            log.warn("Rejected blacklisted token for request to '{}'", request.getRequestURI());
            chain.doFilter(request, response);
            return;
        }

        String username = null;
        try {
            username = jwtService.extractUsername(jwt);
            log.debug("Extracted username '{}' from JWT", username);
        } catch (Exception e) {
            log.error("Failed to extract username from JWT: {}", e.getMessage());
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (!isUserEligibleForAuthentication(userDetails)) {
                    log.warn("User '{}' is not eligible for request authentication", username);
                } else if (jwtService.isAccessTokenValid(jwt, userDetails)) {
                    var authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                    );
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.info("Successfully authenticated user '{}' with roles {}", username, userDetails.getAuthorities());
                } else {
                    log.warn("JWT token is invalid for user '{}'", username);
                }
            } catch (Exception e) {
                log.error("Error authenticating user from JWT: {}", e.getMessage());
            }
        } else {
            if (username == null) {
                log.warn("Username extracted from JWT is null");
            } else {
                log.debug("SecurityContext already holds an authentication for user");
            }
        }

        chain.doFilter(request, response);
    }

    private boolean isUserEligibleForAuthentication(UserDetails userDetails) {
        return userDetails.isAccountNonLocked()
                && userDetails.isEnabled()
                && userDetails.isAccountNonExpired()
                && userDetails.isCredentialsNonExpired();
    }
}
