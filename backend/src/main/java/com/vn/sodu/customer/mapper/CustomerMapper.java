package com.vn.sodu.customer.mapper;

import com.vn.sodu.customer.Customer;
import com.vn.sodu.customer.dto.*;
import com.vn.sodu.customer.royalty.mapper.LoyaltyTierMapper;
import com.vn.sodu.user.mapper.AccountMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomerMapper {

    private final AccountMapper accountMapper;
    private final LoyaltyTierMapper loyaltyTierMapper;

    /**
     * Convert Customer entity to CustomerDTO
     */
    public CustomerDTO toDTO(Customer customer) {
        if (customer == null) {
            return null;
        }
        return CustomerDTO.builder()
                .id(customer.getId())
                .gender(customer.getGender())
                .birthday(customer.getBirthday())
                .province(customer.getProvince())
                .district(customer.getDistrict())
                .ward(customer.getWard())
                .street(customer.getStreet())
                .totalMoney(customer.getTotalMoney())
                .points(customer.getPoints())
                .tier(customer.getTier() != null ? loyaltyTierMapper.toDTO(customer.getTier()) : null)
                .account(customer.getAccount() != null ? accountMapper.toDTO(customer.getAccount()) : null)
                .build();
    }

    /**
     * Convert CustomerDTO to Customer entity
     */
    public Customer toEntity(CustomerDTO dto) {
        if (dto == null) {
            return null;
        }
        return Customer.builder()
                .id(dto.getId())
                .gender(dto.getGender())
                .birthday(dto.getBirthday())
                .province(dto.getProvince())
                .district(dto.getDistrict())
                .ward(dto.getWard())
                .street(dto.getStreet())
                .totalMoney(dto.getTotalMoney())
                .points(dto.getPoints())
                .build();
    }

    /**
     * Convert Customer entity to CustomerResponse
     */
    public CustomerResponse toResponse(Customer customer) {
        if (customer == null) {
            return null;
        }
        return CustomerResponse.builder()
                .id(customer.getId())
                .gender(customer.getGender())
                .birthday(customer.getBirthday() != null ? customer.getBirthday().toString() : null)
                .province(customer.getProvince())
                .district(customer.getDistrict())
                .ward(customer.getWard())
                .street(customer.getStreet())
                .totalMoney(customer.getTotalMoney())
                .points(customer.getPoints())
                .tier(customer.getTier() != null ? loyaltyTierMapper.toDTO(customer.getTier()) : null)
                .email(customer.getAccount() != null ? customer.getAccount().getEmail() : null)
                .fullName(customer.getAccount() != null ? customer.getAccount().getFullName() : null)
                .phone(customer.getAccount() != null ? customer.getAccount().getPhone() : null)
                .build();
    }

    /**
     * Convert CreateCustomerRequest to Customer entity
     */
    public Customer toEntity(CreateCustomerRequest request) {
        if (request == null) {
            return null;
        }
        return Customer.builder()
                .gender(request.getGender())
                .birthday(request.getBirthday())
                .province(request.getProvince())
                .district(request.getDistrict())
                .ward(request.getWard())
                .street(request.getStreet())
                .totalMoney(0.0)
                .points(0)
                .build();
    }

    /**
     * Convert UpdateCustomerRequest to Customer entity
     */
    public Customer toEntity(UpdateCustomerRequest request) {
        if (request == null) {
            return null;
        }
        return Customer.builder()
                .gender(request.getGender())
                .birthday(request.getBirthday())
                .province(request.getProvince())
                .district(request.getDistrict())
                .ward(request.getWard())
                .street(request.getStreet())
                .build();
    }
}
