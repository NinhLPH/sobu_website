package com.vn.sodu.inventory;

/**
 * Type of an inventory ledger entry. Manual types change physical stock
 * ({@code stockRemain}); order types move sellable stock ({@code stockAvailable})
 * without touching physical on-hand quantities.
 */
public enum InventoryAdjustmentType {
    /** Initial physical and sellable quantity set for a product. */
    OPENING_STOCK,
    /** Physical stock received into inventory. */
    STOCK_IN,
    /** Physical stock shipped or consumed. */
    STOCK_OUT,
    /** Absolute correction of physical on-hand quantity. */
    CORRECTION,
    /** Stock damaged or otherwise written off. */
    DAMAGED,
    /** Stock returned into sellable inventory. */
    RETURNED,
    /** Sellable stock reserved by an order at checkout. */
    ORDER_RESERVATION,
    /** Sellable stock released when an order is cancelled or fails. */
    ORDER_RELEASE
}
