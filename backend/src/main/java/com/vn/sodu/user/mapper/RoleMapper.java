package com.vn.sodu.user.mapper;

import com.vn.sodu.user.Role;
import com.vn.sodu.user.dto.*;
import com.vn.sodu.user.dto.*;
import org.springframework.stereotype.Component;

@Component
public class RoleMapper {

    /**
     * Convert Role entity to RoleDTO
     */
    public RoleDTO toDTO(Role role) {
        if (role == null) {
            return null;
        }
        return RoleDTO.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .build();
    }

    /**
     * Convert RoleDTO to Role entity
     */
    public Role toEntity(RoleDTO roleDTO) {
        if (roleDTO == null) {
            return null;
        }
        return Role.builder()
                .id(roleDTO.getId())
                .name(roleDTO.getName())
                .description(roleDTO.getDescription())
                .build();
    }

    /**
     * Convert CreateRoleRequest to Role entity
     */
    public Role toEntity(CreateRoleRequest request) {
        if (request == null) {
            return null;
        }
        return Role.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
    }

    /**
     * Convert EditRoleRequest to Role entity
     */
    public Role toEntity(EditRoleRequest request) {
        if (request == null) {
            return null;
        }
        return Role.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
    }

    /**
     * Convert UpdateRoleRequest to Role entity
     */
    public Role toEntity(UpdateRoleRequest request) {
        if (request == null) {
            return null;
        }
        return Role.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
    }

    /**
     * Convert Role entity to RoleResponse
     */
    public RoleResponse toResponse(Role role) {
        if (role == null) {
            return null;
        }
        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .build();
    }
}
