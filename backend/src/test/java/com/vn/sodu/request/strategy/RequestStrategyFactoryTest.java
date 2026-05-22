package com.vn.sodu.request.strategy;

import com.vn.sodu.request.OrderType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RequestStrategyFactoryTest {

    private final RequestStrategyFactory factory = new RequestStrategyFactory(
            new NormalRequestStrategy(),
            new PreOrderRequestStrategy(),
            new FindingRequestStrategy(),
            new CustomRequestStrategy()
    );

    @Test
    void resolvesCorrectStrategyPerType() {
        assertThat(factory.getStrategy(OrderType.NORMAL)).isInstanceOf(NormalRequestStrategy.class);
        assertThat(factory.getStrategy(OrderType.PREORDER)).isInstanceOf(PreOrderRequestStrategy.class);
        assertThat(factory.getStrategy(OrderType.FINDING)).isInstanceOf(FindingRequestStrategy.class);
        assertThat(factory.getStrategy(OrderType.CUSTOM)).isInstanceOf(CustomRequestStrategy.class);
    }
}
