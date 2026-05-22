package com.vn.sodu.request;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RequestStatusTest {

    @Test
    void terminalStatesAreMarkedCorrectly() {
        assertThat(RequestStatus.APPROVED.isTerminal()).isTrue();
        assertThat(RequestStatus.REJECTED.isTerminal()).isTrue();
        assertThat(RequestStatus.CANCELLED.isTerminal()).isTrue();
        assertThat(RequestStatus.PENDING.isTerminal()).isFalse();
        assertThat(RequestStatus.REVIEWING.isTerminal()).isFalse();
        assertThat(RequestStatus.SOURCING.isTerminal()).isFalse();
        assertThat(RequestStatus.WAITING_CUSTOMER.isTerminal()).isFalse();
    }
}
