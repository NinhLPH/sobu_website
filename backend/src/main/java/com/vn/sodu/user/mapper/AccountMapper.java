package com.vn.sodu.user.mapper;

import com.vn.sodu.user.Account;
import com.vn.sodu.user.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    @Autowired
    private RoleMapper roleMapper;

    /**
     * Convert Account entity to AccountDTO
     */
    public AccountDTO toDTO(Account account) {
        if (account == null) {
            return null;
        }
        return AccountDTO.builder()
                .id(account.getId())
                .role(roleMapper.toDTO(account.getRole()))
                .email(account.getEmail())
                .fullName(account.getFullName())
                .phone(account.getPhone())
                .status(account.getStatus() != null ? account.getStatus().toString() : null)
                .build();
    }

    /**
     * Convert AccountDTO to Account entity
     */
    public Account toEntity(AccountDTO accountDTO) {
        if (accountDTO == null) {
            return null;
        }
        Account account = Account.builder()
                .id(accountDTO.getId())
                .role(roleMapper.toEntity(accountDTO.getRole()))
                .email(accountDTO.getEmail())
                .fullName(accountDTO.getFullName())
                .phone(accountDTO.getPhone())
                .build();
        
        if (accountDTO.getStatus() != null) {
            try {
                account.setStatus(Account.AccountStatus.valueOf(accountDTO.getStatus()));
            } catch (IllegalArgumentException e) {
                account.setStatus(Account.AccountStatus.ACTIVE);
            }
        }
        
        return account;
    }

    /**
     * Convert RegisterRequest to Account entity
     */
    public Account toEntity(RegisterRequest request) {
        if (request == null) {
            return null;
        }
        return Account.builder()
                .email(request.getEmail())
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .status(Account.AccountStatus.INACTIVE)
                .build();
    }

    /**
     * Convert EditAccountRequest to Account entity
     */
    public Account toEntity(EditAccountRequest request) {
        if (request == null) {
            return null;
        }
        return Account.builder()
                .email(request.getEmail())
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .build();
    }

    /**
     * Convert UpdateAccountRequest to Account entity
     */
    public Account toEntity(UpdateAccountRequest request) {
        if (request == null) {
            return null;
        }
        return Account.builder()
                .email(request.getEmail())
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .build();
    }

    /**
     * Convert Account entity to AccountResponse
     */
    public AccountResponse toResponse(Account account) {
        if (account == null) {
            return null;
        }
        return AccountResponse.builder()
                .id(account.getId())
                .email(account.getEmail())
                .fullName(account.getFullName())
                .phone(account.getPhone())
                .role(roleMapper.toDTO(account.getRole()))
                .status(account.getStatus() != null ? account.getStatus().toString() : null)
                .build();
    }

    /**
     * Convert Account entity to RegisterResponse
     */
    public RegisterResponse toRegisterResponse(Account account) {
        if (account == null) {
            return null;
        }
        return RegisterResponse.builder()
                .id(account.getId())
                .email(account.getEmail())
                .fullName(account.getFullName())
                .phone(account.getPhone())
                .role(roleMapper.toDTO(account.getRole()))
                .status(account.getStatus() != null ? account.getStatus().toString() : null)
                .message("Registration successful")
                .build();
    }
}
