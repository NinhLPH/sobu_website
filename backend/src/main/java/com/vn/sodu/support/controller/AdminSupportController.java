package com.vn.sodu.support.controller;

import com.vn.sodu.global.dto.ApiResponseDTO;
import com.vn.sodu.global.dto.PageResponse;
import com.vn.sodu.support.dto.ConversationSummaryDTO;
import com.vn.sodu.support.dto.MessageResponseDTO;
import com.vn.sodu.support.service.SupportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/support/conversations")
@Tag(name = "Admin Support Chat", description = "Staff/admin support conversation management")
@SecurityRequirement(name = "bearerAuth")
@SuppressWarnings("rawtypes")
public class AdminSupportController {

    private final SupportService supportService;

    @GetMapping
    @Operation(summary = "List all support conversations",
            description = "Returns all support conversations ordered by latest message for staff/admin users.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Conversations retrieved",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - staff access required",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<?> listConversations(
            Authentication authentication,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        requireStaff(authentication);
        Pageable pageable = buildPageable(page, size);
        PageResponse<ConversationSummaryDTO> response = PageResponse.from(
                supportService.getStaffConversations(pageable));
        return ResponseEntity.ok(ApiResponseDTO.success(response, "Conversations retrieved", HttpStatus.OK.value()));
    }

    @GetMapping("/{conversationId}/messages")
    @Operation(summary = "Get conversation messages",
            description = "Returns paginated message history for a specific conversation (staff/admin only).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Messages retrieved",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - staff access required",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Conversation not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<?> getConversationMessages(
            Authentication authentication,
            @Parameter(description = "Conversation identifier", required = true, example = "1")
            @PathVariable Long conversationId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        requireStaff(authentication);
        Pageable pageable = buildPageable(page, size);
        PageResponse<MessageResponseDTO> response = PageResponse.from(
                supportService.getMessages(resolveAuthenticatedEmail(authentication), conversationId, pageable));
        return ResponseEntity.ok(ApiResponseDTO.success(response, "Messages retrieved", HttpStatus.OK.value()));
    }

    private void requireStaff(Authentication authentication) {
        if (!isStaff(authentication)) {
            throw new AccessDeniedException("Staff access is required");
        }
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
            if ("ROLE_ADMIN".equals(name) || "ROLE_STAFF".equals(name)) {
                return true;
            }
        }
        return false;
    }

    private String resolveAuthenticatedEmail(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new AccessDeniedException("Authentication is required");
        }
        return authentication.getName();
    }

    private Pageable buildPageable(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size > 0 ? Math.min(size, 100) : 20;
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
