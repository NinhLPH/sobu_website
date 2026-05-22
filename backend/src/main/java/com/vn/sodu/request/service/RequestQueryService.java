package com.vn.sodu.request.service;

import com.vn.sodu.request.Request;
import com.vn.sodu.request.dto.RequestResponseDto;
import com.vn.sodu.request.mapper.RequestResponseMapper;
import com.vn.sodu.request.repo.RequestRepo;
import com.vn.sodu.user.Account;
import com.vn.sodu.user.AccountRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class RequestQueryService {

    private final RequestRepo requestRepo;
    private final AccountRepo accountRepo;
    private final RequestResponseMapper requestResponseMapper;

    @Transactional(readOnly = true)
    public Page<RequestResponseDto> listRequests(Authentication authentication, int page, int size, String sortBy, String sortDirection) {
        Pageable pageable = buildPageable(page, size, sortBy, sortDirection);
        if (isStaff(authentication)) {
            return requestRepo.findAll(pageable).map(requestResponseMapper::toDto);
        }

        String customerPhone = resolveCustomerPhone(authentication);
        return requestRepo.findByCustomerPhone(customerPhone, pageable).map(requestResponseMapper::toDto);
    }

    @Transactional(readOnly = true)
    public RequestResponseDto getRequestDetail(Long requestId, Authentication authentication) {
        if (requestId == null) {
            throw new IllegalArgumentException("Request id is required");
        }

        Request request = isStaff(authentication)
                ? requestRepo.findById(requestId)
                    .orElseThrow(() -> new IllegalArgumentException("Request not found: " + requestId))
                : requestRepo.findByIdAndCustomerPhone(requestId, resolveCustomerPhone(authentication))
                    .orElseThrow(() -> new IllegalArgumentException("Request not found: " + requestId));

        return requestResponseMapper.toDto(request);
    }

    private Pageable buildPageable(int page, int size, String sortBy, String sortDirection) {
        String safeSortBy = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy;
        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(sortDirection == null ? "DESC" : sortDirection);
        } catch (IllegalArgumentException ex) {
            direction = Sort.Direction.DESC;
        }
        int safePage = Math.max(page, 0);
        int safeSize = size > 0 ? Math.min(size, 100) : 20;
        return PageRequest.of(safePage, safeSize, Sort.by(direction, safeSortBy));
    }

    private boolean isStaff(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (authority == null || authority.getAuthority() == null) {
                continue;
            }
            String name = authority.getAuthority().toUpperCase(Locale.ROOT);
            if (name.equals("ROLE_ADMIN") || name.equals("ROLE_STAFF")) {
                return true;
            }
        }
        return false;
    }

    private String resolveCustomerPhone(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new AccessDeniedException("Authentication is required");
        }

        Account account = accountRepo.findByEmail(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Authenticated account not found"));

        if (account.getPhone() == null || account.getPhone().isBlank()) {
            throw new AccessDeniedException("Authenticated account does not have a phone number");
        }
        return account.getPhone().trim();
    }
}
