package com.vn.sodu.request.strategy;

import com.vn.sodu.request.OrderType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RequestStrategyFactory {

    private final NormalRequestStrategy normalRequestStrategy;
    private final PreOrderRequestStrategy preOrderRequestStrategy;
    private final FindingRequestStrategy findingRequestStrategy;
    private final CustomRequestStrategy customRequestStrategy;

    public RequestStrategy getStrategy(OrderType type) {
        if (type == null) {
            throw new IllegalArgumentException("Request type is required");
        }

        return switch (type) {
            case NORMAL -> normalRequestStrategy;
            case PREORDER -> preOrderRequestStrategy;
            case FINDING -> findingRequestStrategy;
            case CUSTOM -> customRequestStrategy;
        };
    }
}
