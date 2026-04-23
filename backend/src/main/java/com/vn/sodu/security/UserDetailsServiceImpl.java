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
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 1. Tìm account bằng email
        Account account = accountRepo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng với email: " + email));

        // 2. Kiểm tra dữ liệu bắt buộc để tránh lỗi IllegalArgumentException
        String password = account.getPasswordHash();
        if (password == null || password.isEmpty()) {
            throw new RuntimeException("Mật khẩu của người dùng này chưa được thiết lập trong hệ thống.");
        }

        // 3. Trả về đối tượng User của Spring Security
        return User.builder()
                .username(account.getEmail()) // Ở đây username của Spring Security sẽ chứa giá trị email
                .password(password)
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
