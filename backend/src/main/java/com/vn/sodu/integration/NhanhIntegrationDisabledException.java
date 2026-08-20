package com.vn.sodu.integration;

/**
 * Thrown when an action requires the Nhanh integration but it is disabled.
 * Mapped to HTTP 403 by the global exception handler.
 */
public class NhanhIntegrationDisabledException extends RuntimeException {

    public NhanhIntegrationDisabledException() {
        super("Nhanh integration is disabled (local mode)");
    }
}
