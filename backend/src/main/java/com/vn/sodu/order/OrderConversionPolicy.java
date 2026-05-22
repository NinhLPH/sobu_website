package com.vn.sodu.order;

import com.vn.sodu.request.OrderType;
import com.vn.sodu.request.Request;
import com.vn.sodu.request.RequestItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OrderConversionPolicy {

    private final OrderRepository orderRepository;

    /**
     * Checks if an internal order already exists for this request.
     */
    public Optional<Order> getExistingOrder(Request request) {
        if (request.getId() == null) {
            return Optional.empty();
        }
        return orderRepository.findByRequestId(request.getId());
    }

    /**
     * Validates if a request and resolved customer meet all requirements for conversion.
     */
    public void validateForConversion(Request request, ResolvedOrderCustomer customer) {
        validateCustomer(customer);
        validateItems(request);
    }

    private void validateCustomer(ResolvedOrderCustomer customer) {
        if (customer == null) {
            throw new IllegalArgumentException("Cannot convert request to order: Customer resolution failed.");
        }
        if (customer.getFullName() == null || customer.getFullName().isBlank()) {
            throw new IllegalArgumentException("Cannot convert request to order: Customer fullName is required.");
        }
        if (customer.getPhone() == null || customer.getPhone().isBlank()) {
            throw new IllegalArgumentException("Cannot convert request to order: Customer phone is required.");
        }
    }

    private void validateItems(Request request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cannot convert request to order: Request has no items.");
        }

        if (request.getType() == OrderType.NORMAL) {
            for (RequestItem item : request.getItems()) {
                if (item.getNhanhProductId() == null || item.getNhanhProductId().isBlank()) {
                    throw new IllegalArgumentException("NORMAL order requires nhanhProductId for all items.");
                }
                if (item.getPrice() == null || item.getPrice().compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalArgumentException("NORMAL order requires a valid price for all items.");
                }
                if (item.getQuantity() == null || item.getQuantity() <= 0) {
                    throw new IllegalArgumentException("NORMAL order requires a positive quantity for all items.");
                }
            }
        }
    }
}
