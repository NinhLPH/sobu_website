package com.vn.sodu.inventory;

import com.vn.sodu.audit.AuditAction;
import com.vn.sodu.audit.AuditService;
import com.vn.sodu.global.exception.BadRequestException;
import com.vn.sodu.global.exception.NotFoundException;
import com.vn.sodu.global.dto.PageResponse;
import com.vn.sodu.inventory.dto.InventoryAdjustmentDto;
import com.vn.sodu.inventory.dto.InventoryBalanceDto;
import com.vn.sodu.inventory.dto.InventoryProductDto;
import com.vn.sodu.order.Order;
import com.vn.sodu.order.OrderItem;
import com.vn.sodu.product.Product;
import com.vn.sodu.product.repo.ProductRepo;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The sole writer of product stock and inventory ledger rows. Physical stock
 * ({@code stockRemain}) changes only through manual adjustments; sellable stock
 * ({@code stockAvailable}) is reserved at order creation and released when the
 * order is cancelled or its payment becomes terminal.
 */
@Service
@RequiredArgsConstructor
public class InventoryService {

    private static final String ACTOR_SYSTEM = "system";

    private final ProductRepo productRepo;
    private final InventoryLedgerRepository ledgerRepository;
    private final AuditService auditService;

    @Transactional
    public InventoryAdjustmentDto setOpeningStock(Long productId, Double quantity, String note) {
        Product product = requireProduct(productId);
        requireNonNegative(quantity, "Opening stock quantity must be greater than or equal to 0");

        double before = safe(product.getStockRemain());
        product.setStockRemain(quantity);
        product.setStockAvailable(quantity);
        product.setUpdatedAt(java.time.LocalDateTime.now());
        productRepo.save(product);

        InventoryAdjustment entry = record(
                productId, InventoryAdjustmentType.OPENING_STOCK, quantity, quantity, null, null, note);
        auditService.record(AuditAction.INVENTORY_ADJUSTMENT, "PRODUCT", String.valueOf(productId),
                String.valueOf(before), String.valueOf(quantity), "Opening stock set");
        return toDto(entry);
    }

    @Transactional
    public InventoryAdjustmentDto adjust(Long productId, InventoryAdjustmentType type, Double quantity, String note) {
        if (type == null) {
            throw new BadRequestException("Inventory adjustment type is required");
        }
        switch (type) {
            case OPENING_STOCK -> throw new BadRequestException("Use the opening-stock endpoint to set opening stock");
            case ORDER_RESERVATION, ORDER_RELEASE ->
                    throw new BadRequestException("Order adjustments are applied by the order lifecycle");
            default -> { }
        }

        Product product = requireProduct(productId);
        double current = safe(product.getStockRemain());
        double delta;
        switch (type) {
            case STOCK_IN, RETURNED -> delta = requirePositive(quantity);
            case STOCK_OUT, DAMAGED -> delta = -requirePositive(quantity);
            case CORRECTION -> {
                requireNonNegative(quantity, "Correction target must be greater than or equal to 0");
                delta = quantity - current;
            }
            default -> throw new BadRequestException("Unsupported inventory adjustment type: " + type);
        }

        double newRemain = round(current + delta);
        double newAvailable = round(safe(product.getStockAvailable()) + delta);
        validateBalance(newRemain, newAvailable);

        product.setStockRemain(newRemain);
        product.setStockAvailable(newAvailable);
        product.setUpdatedAt(java.time.LocalDateTime.now());
        productRepo.save(product);

        InventoryAdjustment entry = record(productId, type, delta, newRemain, null, null, note);
        auditService.record(AuditAction.INVENTORY_ADJUSTMENT, "PRODUCT", String.valueOf(productId),
                String.valueOf(current), String.valueOf(newRemain),
                (note == null || note.isBlank() ? "" : note + " — ") + type);
        return toDto(entry);
    }

    /**
     * Atomically decrements sellable stock for every order item that resolves to
     * a local product. Idempotent per order + product: a second call for the same
     * order does not double-reserve. Throws {@link InsufficientStockException} and
     * rolls back the surrounding transaction if any item cannot be fulfilled.
     */
    @Transactional
    public List<InventoryAdjustmentDto> reserveForOrder(Order order) {
        List<InventoryAdjustmentDto> result = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : aggregateQuantities(order).entrySet()) {
            Long productId = entry.getKey();
            int quantity = entry.getValue();
            if (ledgerRepository.existsByOrderIdAndTypeAndProductId(order.getId(), InventoryAdjustmentType.ORDER_RESERVATION, productId)) {
                continue;
            }
            Product product = requireProduct(productId);
            double available = safe(product.getStockAvailable());
            if (available < quantity) {
                throw new InsufficientStockException(productId, quantity, available);
            }
            double newAvailable = round(available - quantity);
            product.setStockAvailable(newAvailable);
            product.setUpdatedAt(java.time.LocalDateTime.now());
            productRepo.save(product);
            result.add(toDto(record(productId, InventoryAdjustmentType.ORDER_RESERVATION, -quantity, newAvailable,
                    order.getId(), order.getOrderCode(), null)));
        }
        return result;
    }

    /** Releases sellable stock reserved by an order. Idempotent per order + product. */
    @Transactional
    public List<InventoryAdjustmentDto> releaseForOrder(Order order) {
        if (order == null || order.getId() == null) {
            return List.of();
        }
        List<InventoryAdjustmentDto> result = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : aggregateQuantities(order).entrySet()) {
            Long productId = entry.getKey();
            int quantity = entry.getValue();
            if (ledgerRepository.existsByOrderIdAndTypeAndProductId(order.getId(), InventoryAdjustmentType.ORDER_RELEASE, productId)) {
                continue;
            }
            Product product = requireProduct(productId);
            double remain = safe(product.getStockRemain());
            double available = safe(product.getStockAvailable());
            double newAvailable = round(Math.min(remain, available + quantity));
            product.setStockAvailable(newAvailable);
            product.setUpdatedAt(java.time.LocalDateTime.now());
            productRepo.save(product);
            result.add(toDto(record(productId, InventoryAdjustmentType.ORDER_RELEASE, quantity, newAvailable,
                    order.getId(), order.getOrderCode(), null)));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public InventoryBalanceDto getBalance(Long productId) {
        if (productId == null) {
            throw new BadRequestException("Product id is required");
        }
        // Use plain findById (no lock) — a PESSIMISTIC_WRITE lock (FOR UPDATE) is
        // illegal inside a read-only transaction and is unnecessary for a balance query.
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + productId));
        double remain = safe(product.getStockRemain());
        double available = safe(product.getStockAvailable());
        return InventoryBalanceDto.builder()
                .productId(product.getId())
                .stockRemain(remain)
                .stockAvailable(available)
                .reserved(round(remain - available))
                .build();
    }

    @Transactional(readOnly = true)
    public List<InventoryAdjustmentDto> getLedger(Long productId) {
        if (productId == null) {
            throw new BadRequestException("Product id is required");
        }
        // Use plain findById (no lock) — same reason as getBalance.
        if (!productRepo.existsById(productId)) {
            throw new NotFoundException("Product not found with id: " + productId);
        }
        return ledgerRepository.findByProductIdOrderByIdDesc(productId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<InventoryProductDto> getInventoryProducts(
            String search,
            String stockStatus,
            int page,
            int pageSize,
            String sortBy,
            String sortDirection
    ) {
        int safePage = Math.max(0, page);
        int safeSize = pageSize <= 0 ? 20 : Math.min(pageSize, 100);

        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String safeSortBy = (sortBy == null || sortBy.isBlank()) ? "id" : sortBy.trim();
        if (!List.of("id", "name", "code", "stockRemain", "stockAvailable", "updatedAt", "createdAt").contains(safeSortBy)) {
            safeSortBy = "id";
        }
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(direction, safeSortBy));

        Specification<Product> spec = (root, query, cb) -> {
            Predicate predicate = cb.conjunction();

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate searchPredicate = cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("code")), pattern),
                        cb.like(cb.lower(root.get("barcode")), pattern),
                        cb.like(cb.lower(root.get("categoryName")), pattern),
                        cb.like(cb.lower(root.get("brandName")), pattern)
                );
                predicate = cb.and(predicate, searchPredicate);
            }

            if (stockStatus != null && !stockStatus.isBlank()) {
                switch (stockStatus.toUpperCase(Locale.ROOT)) {
                    case "OUT_OF_STOCK" -> predicate = cb.and(predicate,
                            cb.or(cb.isNull(root.<Double>get("stockAvailable")), cb.lessThanOrEqualTo(root.<Double>get("stockAvailable"), 0d)));
                    case "IN_STOCK" -> predicate = cb.and(predicate,
                            cb.and(cb.isNotNull(root.<Double>get("stockAvailable")), cb.greaterThan(root.<Double>get("stockAvailable"), 0d)));
                    case "LOW_STOCK" -> predicate = cb.and(predicate,
                            cb.and(
                                    cb.isNotNull(root.<Double>get("stockAvailable")),
                                    cb.greaterThan(root.<Double>get("stockAvailable"), 0d),
                                    cb.lessThanOrEqualTo(root.<Double>get("stockAvailable"), 5d)
                            ));
                    default -> { }
                }
            }

            return predicate;
        };

        Page<InventoryProductDto> dtoPage = productRepo.findAll(spec, pageable)
                .map(this::toInventoryProductDto);

        return PageResponse.from(dtoPage);
    }

    public InventoryProductDto toInventoryProductDto(Product product) {
        if (product == null) {
            return null;
        }
        double remain = safe(product.getStockRemain());
        double available = safe(product.getStockAvailable());
        double reserved = round(Math.max(0d, remain - available));

        return InventoryProductDto.builder()
                .id(product.getId())
                .productId(product.getId())
                .externalId(product.getExternalId())
                .name(product.getName())
                .code(product.getCode())
                .sku(product.getCode())
                .barcode(product.getBarcode())
                .avatarImage(product.getAvatarImage())
                .categoryId(product.getCategoryId())
                .categoryName(product.getCategoryName())
                .brandId(product.getBrandId())
                .brandName(product.getBrandName())
                .price(product.getRetailPrice())
                .retailPrice(product.getRetailPrice())
                .stockRemain(remain)
                .stockAvailable(available)
                .reserved(reserved)
                .status(product.getStatus())
                .active(product.getActive())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    private Map<Long, Integer> aggregateQuantities(Order order) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        if (order == null || order.getItems() == null) {
            return quantities;
        }
        for (OrderItem item : order.getItems()) {
            if (item == null || item.getProductId() == null) {
                continue;
            }
            quantities.merge(item.getProductId(), item.getQuantity() == null ? 0 : item.getQuantity(), Integer::sum);
        }
        return quantities;
    }

    private InventoryAdjustment record(Long productId, InventoryAdjustmentType type, double delta,
                                       double balanceAfter, Long orderId, String orderCode, String note) {
        InventoryAdjustment entry = InventoryAdjustment.builder()
                .productId(productId)
                .type(type)
                .quantityDelta(delta)
                .balanceAfter(balanceAfter)
                .orderId(orderId)
                .orderCode(orderCode)
                .note(note)
                .actor(resolveActor())
                .build();
        return ledgerRepository.save(entry);
    }

    private Product requireProduct(Long productId) {
        if (productId == null) {
            throw new BadRequestException("Product id is required");
        }
        return productRepo.findByIdForUpdate(productId)
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + productId));
    }

    private void validateBalance(double remain, double available) {
        if (remain < 0 || available < 0) {
            throw new BadRequestException("Inventory cannot go below zero");
        }
        if (available > remain) {
            throw new BadRequestException("Sellable stock cannot exceed physical stock");
        }
    }

    private double requirePositive(Double value) {
        if (value == null || value <= 0) {
            throw new BadRequestException("Adjustment quantity must be greater than 0");
        }
        return value;
    }

    private void requireNonNegative(Double value, String message) {
        if (value == null || value < 0) {
            throw new BadRequestException(message);
        }
    }

    private double safe(Double value) {
        return value == null ? 0d : value;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String resolveActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            return ACTOR_SYSTEM;
        }
        return authentication.getName();
    }

    private InventoryAdjustmentDto toDto(InventoryAdjustment entry) {
        if (entry == null) {
            return null;
        }
        return InventoryAdjustmentDto.builder()
                .id(entry.getId())
                .productId(entry.getProductId())
                .type(entry.getType())
                .quantityDelta(entry.getQuantityDelta())
                .balanceAfter(entry.getBalanceAfter())
                .orderId(entry.getOrderId())
                .orderCode(entry.getOrderCode())
                .note(entry.getNote())
                .actor(entry.getActor())
                .createdAt(entry.getCreatedAt())
                .build();
    }
}