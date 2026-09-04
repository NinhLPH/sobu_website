package com.vn.sodu.location;

/**
 * Thrown when a write uses the retired Nhanh location contract (v1) or an
 * unknown location contract version. Local mode only accepts the canonical
 * Sobu administrative address contract (v2). It is mapped to HTTP 409 by the
 * global exception handler so the caller receives a clear migration/upgrade
 * response instead of a partially translated address being persisted.
 */
public class AddressMigrationRequiredException extends RuntimeException {

    public AddressMigrationRequiredException(String message) {
        super(message);
    }
}
