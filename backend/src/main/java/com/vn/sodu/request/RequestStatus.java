package com.vn.sodu.request;

public enum RequestStatus {
    PENDING,
    REVIEWING,
    SOURCING,
    WAITING_CUSTOMER,
    APPROVED,
    REJECTED,
    CANCELLED;

    public boolean isTerminal() {
        return this == APPROVED || this == REJECTED || this == CANCELLED;
    }
}
