package com.vn.sodu.security;

import com.vn.sodu.user.Account;
import com.vn.sodu.user.AccountRepo;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final AccountRepo accountRepo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Account account = accountRepo.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));

        return User.builder()
                .username(account.getUsername())
                .password(account.getPasswordHash())
                .authorities(getAuthorities(account))
                .accountExpired(false)
                .accountLocked(!account.getStatus().equals(Account.AccountStatus.ACTIVE))
                .credentialsExpired(false)
                .disabled(account.getStatus().equals(Account.AccountStatus.BANNED))
                .build();
    }

    /**
     * Get authorities based on user role
     */
    private Collection<? extends GrantedAuthority> getAuthorities(Account account) {
        Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
        
        if (account.getRole() != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + account.getRole().getName().toUpperCase()));
        } else {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }
        
        return authorities;
    }
}
