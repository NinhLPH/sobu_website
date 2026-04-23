package com.vn.sodu.customer.service;

import com.vn.sodu.customer.Customer;
import com.vn.sodu.customer.CustomerRepo;
import com.vn.sodu.customer.dto.CreateCustomerRequest;
import com.vn.sodu.customer.dto.UpdateCustomerRequest;
import com.vn.sodu.user.Account;
import com.vn.sodu.user.AccountRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepo customerRepo;
    private final AccountRepo accountRepo;

    @Transactional
    public Customer createCustomer(Long accountId, CreateCustomerRequest request) {
        Account account = accountRepo.findById(accountId).orElseThrow(()-> new RuntimeException("Account not found"));
        // 2. check đã có customer chưa
        if (customerRepo.existsByAccountId(accountId)) {
            throw new RuntimeException("Customer already exists");
        }

        // 3. tạo customer
        Customer customer = Customer.builder()
                .account(account)
                .gender(request.getGender())
                .birthday(request.getBirthday())
                .province(request.getProvince())
                .district(request.getDistrict())
                .ward(request.getWard())
                .street(request.getStreet())
                .totalMoney(0.0)
                .points(0)
                .build();

        return customerRepo.save(customer);
    }

    @Transactional
    public Customer updateCustomer(Long accountId, UpdateCustomerRequest request) {

        // 1. tìm customer theo account
        Customer customer = customerRepo.findByAccountId(accountId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        // 2. update từng field (PATCH-style)
        if (request.getGender() != null) {
            customer.setGender(request.getGender());
        }

        if (request.getBirthday() != null) {
            customer.setBirthday(request.getBirthday());
        }

        if (request.getProvince() != null) {
            customer.setProvince(request.getProvince());
        }

        if (request.getDistrict() != null) {
            customer.setDistrict(request.getDistrict());
        }

        if (request.getWard() != null) {
            customer.setWard(request.getWard());
        }

        if (request.getStreet() != null) {
            customer.setStreet(request.getStreet());
        }

        return customerRepo.save(customer);
    }
}
