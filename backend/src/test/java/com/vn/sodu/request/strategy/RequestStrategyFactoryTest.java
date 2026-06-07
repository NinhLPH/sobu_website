package com.vn.sodu.request.strategy;

import com.vn.sodu.request.OrderType;
import com.vn.sodu.product.repo.ProductRepo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RequestStrategyFactoryTest {

    private final RequestStrategyFactory factory = new RequestStrategyFactory(
            new NormalRequestStrategy(),
            new PreOrderRequestStrategy(mock(ProductRepo.class)),
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
