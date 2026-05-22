package com.vn.sodu.request.controller;

import com.vn.sodu.global.dto.ApiResponseDTO;
import com.vn.sodu.global.dto.PageResponse;
import com.vn.sodu.request.Request;
import com.vn.sodu.request.dto.CreateRequestDto;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping({"/api/v1/requests", "/api/v1/request", "/api/requests", "/api/request"})
@Tag(name = "Request Workflow", description = "Authenticated request lifecycle endpoints")
@SecurityRequirement(name = "bearerAuth")
@SuppressWarnings("rawtypes")
public class RequestController {

    private final RequestWorkflowService requestWorkflowService;
    private final RequestQueryService requestQueryService;
    private final RequestResponseMapper requestResponseMapper;

    @GetMapping
    @Operation(
            summary = "List requests",
            description = "Returns request list for the authenticated actor. Customer only sees their own requests, staff/admin sees all requests."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Requests retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
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
        PageResponse<RequestResponseDto> response = PageResponse.from(
                requestQueryService.listRequests(authentication, page, size, sortBy, sortDirection)
        );
        return ResponseEntity.ok(ApiResponseDTO.success(response, "Requests retrieved", HttpStatus.OK.value()));
    }

    @GetMapping("/me")
    @Operation(
            summary = "Get my requests",
            description = "Returns the authenticated customer's own requests using the account phone binding."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Requests retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<?> getMyRequests(
            Authentication authentication,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(name = "sortDirection", defaultValue = "DESC") String sortDirection
    ) {
        PageResponse<RequestResponseDto> response = PageResponse.from(
                requestQueryService.listRequests(authentication, page, size, sortBy, sortDirection)
        );
        return ResponseEntity.ok(ApiResponseDTO.success(response, "My requests retrieved", HttpStatus.OK.value()));
    }

    @GetMapping("/me/{requestId}")
    @Operation(
            summary = "Get my request detail",
            description = "Returns the authenticated customer's own request detail using the account phone binding."
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
    public ResponseEntity<?> getMyRequestDetail(
            @Parameter(description = "Request identifier", required = true, example = "1")
            @PathVariable Long requestId,
            Authentication authentication
    ) {
        RequestResponseDto response = requestQueryService.getRequestDetail(requestId, authentication);
        return ResponseEntity.ok(ApiResponseDTO.success(response, "Request retrieved", HttpStatus.OK.value()));
    }

    @PostMapping
    @Operation(
            summary = "Create request",
            description = "Creates a new negotiation request with normalized items and attachments."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Request created successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<?> createRequest(@Valid @RequestBody CreateRequestDto dto) {
        Request request = requestWorkflowService.createRequest(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success(requestResponseMapper.toDto(request), "Request created", HttpStatus.CREATED.value()));
    }

    @PutMapping("/{requestId}")
    @Operation(
            summary = "Update request",
            description = "Updates an existing request when the current status allows editing."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request updated successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload",
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
            @Valid @RequestBody UpdateRequestDto dto
    ) {
        Request request = requestWorkflowService.updateRequest(requestId, dto);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponseDTO.success(requestResponseMapper.toDto(request), "Request updated", HttpStatus.OK.value()));
    }
}
