package com.vn.sodu.request.controller;

import com.vn.sodu.global.dto.ApiResponseDTO;
import com.vn.sodu.global.dto.PageResponse;
import com.vn.sodu.request.Request;
import com.vn.sodu.request.dto.ProcessRequestDto;
import com.vn.sodu.request.dto.RequestResponseDto;
import com.vn.sodu.request.dto.UpdateRequestDto;
import com.vn.sodu.request.mapper.RequestResponseMapper;
import com.vn.sodu.request.service.RequestQueryService;
import com.vn.sodu.request.service.RequestWorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.function.Supplier;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping({"/api/v1/admin/requests", "/api/admin/requests"})
@Tag(name = "Admin Request Workflow", description = "Staff-only request endpoints")
@SecurityRequirement(name = "bearerAuth")
@SuppressWarnings("rawtypes")
public class AdminRequestController {

    private final RequestQueryService requestQueryService;
    private final RequestWorkflowService requestWorkflowService;
    private final RequestResponseMapper requestResponseMapper;

    @GetMapping
    @Operation(
            summary = "List all requests",
            description = "Returns every request for staff/admin users."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Requests retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<?> listRequests(
            Authentication authentication,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(name = "sortDirection", defaultValue = "DESC") String sortDirection
    ) {
        requireStaff(authentication);
        PageResponse<RequestResponseDto> response = PageResponse.from(
                requestQueryService.listRequests(authentication, page, size, sortBy, sortDirection)
        );
        return ResponseEntity.ok(ApiResponseDTO.success(response, "Requests retrieved", HttpStatus.OK.value()));
    }

    @GetMapping("/{requestId}")
    @Operation(
            summary = "Get request detail",
            description = "Returns any request detail for staff/admin users."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Request not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<?> getRequestDetail(
            @Parameter(description = "Request identifier", required = true, example = "1")
            @PathVariable Long requestId,
            Authentication authentication
    ) {
        requireStaff(authentication);
        RequestResponseDto response = requestQueryService.getRequestDetail(requestId, authentication);
        return ResponseEntity.ok(ApiResponseDTO.success(response, "Request retrieved", HttpStatus.OK.value()));
    }

    @PutMapping("/{requestId}")
    @Operation(
            summary = "Update any request",
            description = "Allows staff/admin to update request data within workflow edit rules."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request updated successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Request not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Request cannot be updated in current status",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<?> updateRequest(
            @Parameter(description = "Request identifier", required = true, example = "1")
            @PathVariable Long requestId,
            @Valid @RequestBody UpdateRequestDto dto,
            Authentication authentication
    ) {
        requireStaff(authentication);
        Request request = requestWorkflowService.updateRequestAsAdmin(requestId, dto);
        return ResponseEntity.ok(ApiResponseDTO.success(requestResponseMapper.toDto(request), "Request updated", HttpStatus.OK.value()));
    }

    @PostMapping("/{requestId}/process")
    @Operation(
            summary = "Process request",
            description = "Moves a request to the next workflow status using the authenticated staff user as actor."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request processed successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid processing payload",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Request not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Invalid status transition",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<?> processRequest(
            @Parameter(description = "Request identifier", required = true, example = "1")
            @PathVariable Long requestId,
            @Valid @RequestBody ProcessRequestDto dto,
            Authentication authentication
    ) {
        requireStaff(authentication);
        Request request = requestWorkflowService.processRequest(requestId, dto.getTargetStatus(), authentication.getName(), dto.getNote());
        return ResponseEntity.ok(ApiResponseDTO.success(requestResponseMapper.toDto(request), "Request processed", HttpStatus.OK.value()));
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
            if (name.equals("ROLE_ADMIN") || name.equals("ROLE_STAFF")) {
                return true;
            }
        }
        return false;
    }
}
