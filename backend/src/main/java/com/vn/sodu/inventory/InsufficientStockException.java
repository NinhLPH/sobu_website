package com.vn.sodu.inventory;

/**
 * Thrown when an order reservation cannot be fulfilled because the product's
 * sellable stock ({@code stockAvailable}) is lower than the requested quantity.
 * Mapped to {@code 409 INSUFFICIENT_STOCK} by the global exception handler.
 */
public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String message) {
        super(message);
    }

    public InsufficientStockException(Long productId, int requested, double available) {
        this("Insufficient stock for product " + productId
                + ": requested " + requested + ", available " + available);
    }
}