package com.vn.sodu.order;

import com.vn.sodu.user.Account;
import com.vn.sodu.user.AccountRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderCustomerResolver {
    
    private final AccountRepo accountRepo;
    
    @Transactional(readOnly = true)
    public Optional<ResolvedOrderCustomer> resolveByPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return Optional.empty();
        }
        
        return accountRepo.findByPhone(phone)
            .map(this::mapToResolvedCustomer);
    }
    
    private ResolvedOrderCustomer mapToResolvedCustomer(Account account) {
        var builder = ResolvedOrderCustomer.builder()
            .fullName(account.getFullName())
            .email(account.getEmail())
            .phone(account.getPhone());
            
        var customer = account.getCustomer();
        if (customer != null) {
            builder.street(customer.getStreet())
                   .province(customer.getProvince())
                   .district(customer.getDistrict())
                   .ward(customer.getWard());
        }
        
        return builder.build();
    }
}
