package com.vn.sodu.request.policy;

import com.vn.sodu.request.RequestStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestTransitionPolicyTest {

    private final RequestTransitionPolicy policy = new RequestTransitionPolicy();

    @Test
    void allowsNegotiationWorkflowTransitions() {
        assertThat(policy.canTransition(RequestStatus.PENDING, RequestStatus.REVIEWING)).isTrue();
        assertThat(policy.canTransition(RequestStatus.REVIEWING, RequestStatus.WAITING_CUSTOMER)).isTrue();
        assertThat(policy.canTransition(RequestStatus.SOURCING, RequestStatus.APPROVED)).isTrue();
        assertThat(policy.canTransition(RequestStatus.WAITING_CUSTOMER, RequestStatus.CANCELLED)).isTrue();
    }

    @Test
    void blocksInvalidTransitions() {
        assertThat(policy.canTransition(RequestStatus.APPROVED, RequestStatus.REVIEWING)).isFalse();
        assertThat(policy.canTransition(RequestStatus.REJECTED, RequestStatus.PENDING)).isFalse();
        assertThat(policy.canTransition(null, RequestStatus.PENDING)).isFalse();
        assertThat(policy.canTransition(RequestStatus.PENDING, null)).isFalse();
    }

    @Test
    void validateTransitionThrowsForInvalidMove() {
        assertThatThrownBy(() -> policy.validateTransition(RequestStatus.APPROVED, RequestStatus.REVIEWING))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid request transition");
    }

    @Test
    void exposesAllowedTransitionsPerState() {
        assertThat(policy.allowedTransitions(RequestStatus.PENDING))
                .containsExactlyInAnyOrder(
                        RequestStatus.REVIEWING,
                        RequestStatus.SOURCING,
                        RequestStatus.REJECTED,
                        RequestStatus.CANCELLED
                );
    }
}
