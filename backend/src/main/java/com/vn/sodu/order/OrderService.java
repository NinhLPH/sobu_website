package com.vn.sodu.order;

import com.vn.sodu.request.Request;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderConversionPolicy orderConversionPolicy;
    private final OrderCustomerResolver orderCustomerResolver;
    private final RequestToOrderMapper requestToOrderMapper;

    @Transactional
    public Order createFromApprovedRequest(Request request) {
        // 1. Check idempotency
        Optional<Order> existingOrder = orderConversionPolicy.getExistingOrder(request);
        if (existingOrder.isPresent()) {
            log.info("Order already exists for request {}, returning existing order.", request.getId());
            return existingOrder.get();
        }

        // 2. Resolve customer
        ResolvedOrderCustomer customer = orderCustomerResolver.resolveByPhone(request.getCustomerPhone())
            .orElseThrow(() -> new IllegalArgumentException("Customer resolution failed for phone: " + request.getCustomerPhone()));

        // 3. Validate for conversion
        orderConversionPolicy.validateForConversion(request, customer);

        // 4. Map to internal Order
        Order newOrder = requestToOrderMapper.mapToOrder(request, customer);

        // 5. Save order
        return orderRepository.save(newOrder);
    }
}
