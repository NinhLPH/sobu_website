package com.vn.sodu.support.controller;

import com.vn.sodu.global.dto.ApiResponseDTO;
import com.vn.sodu.global.dto.PageResponse;
import com.vn.sodu.global.exception.NotFoundException;
import com.vn.sodu.support.dto.ConversationSummaryDTO;
import com.vn.sodu.support.dto.MessageResponseDTO;
import com.vn.sodu.support.service.SupportService;
import com.vn.sodu.user.Account;
import com.vn.sodu.user.AccountRepo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/support")
@Tag(name = "Support Chat", description = "Customer support conversation endpoints")
@SecurityRequirement(name = "bearerAuth")
@SuppressWarnings("rawtypes")
public class SupportController {

    private final SupportService supportService;
    private final AccountRepo accountRepo;

    @GetMapping("/conversation")
    @Operation(summary = "Get or create support conversation",
            description = "Returns the authenticated customer's support conversation, creating one if it does not exist.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Conversation retrieved or created",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<?> getOrCreateConversation(Authentication authentication) {
        Account account = resolveAccount(authentication);
        if (supportService.isStaff(account)) {
            throw new AccessDeniedException("Staff accounts do not have a personal support conversation");
        }
        ConversationSummaryDTO dto = supportService.toSummary(
                supportService.getOrCreateConversation(account));
        return ResponseEntity.ok(ApiResponseDTO.success(dto, "Conversation retrieved", HttpStatus.OK.value()));
    }

    @GetMapping("/conversation/messages")
    @Operation(summary = "Get message history",
            description = "Returns the authenticated customer's support message history with pagination.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Messages retrieved",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<?> getMyMessages(
            Authentication authentication,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        Account account = resolveAccount(authentication);
        Pageable pageable = buildPageable(page, size);
        PageResponse<MessageResponseDTO> response = PageResponse.from(
                supportService.getMyMessages(account, pageable));
        return ResponseEntity.ok(ApiResponseDTO.success(response, "Messages retrieved", HttpStatus.OK.value()));
    }

    private Account resolveAccount(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new AccessDeniedException("Authentication is required");
        }
        return accountRepo.findByEmail(authentication.getName())
                .orElseThrow(() -> new NotFoundException("Authenticated account not found"));
    }

    private Pageable buildPageable(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size > 0 ? Math.min(size, 100) : 20;
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
