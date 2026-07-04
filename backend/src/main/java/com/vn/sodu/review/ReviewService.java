package com.vn.sodu.review;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.sodu.global.exception.ForbiddenOperationException;
import com.vn.sodu.global.exception.NotFoundException;
import com.vn.sodu.order.Order;
import com.vn.sodu.order.OrderCustomerEmailMatcher;
import com.vn.sodu.order.OrderItem;
import com.vn.sodu.order.OrderStatus;
import com.vn.sodu.order.repo.OrderRepository;
import com.vn.sodu.product.Product;
import com.vn.sodu.product.repo.ProductRepo;
import com.vn.sodu.review.dto.CreateReviewRequest;
import com.vn.sodu.review.dto.ReviewEligibilityResponse;
import com.vn.sodu.review.dto.ReviewResponseDto;
import com.vn.sodu.review.dto.UpdateReviewStatusRequest;
import com.vn.sodu.review.dto.ReplyReviewRequest;
import com.vn.sodu.user.Account;
import com.vn.sodu.user.AccountRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepo productRepo;
    private final OrderRepository orderRepository;
    private final AccountRepo accountRepo;
    private final ObjectMapper objectMapper;

    @Transactional
    public ReviewResponseDto createReview(CreateReviewRequest request, String accountEmail) {
        Account account = resolveAccount(accountEmail);

        Product product = productRepo.findById(request.getProductId())
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + request.getProductId()));

        Order order = orderRepository.findWithItemsAndRequestById(request.getOrderId())
                .orElseThrow(() -> new NotFoundException("Order not found with id: " + request.getOrderId()));

        if (!OrderCustomerEmailMatcher.matches(order.getCustomerEmail(), account.getEmail())) {
            throw new ForbiddenOperationException("Order does not belong to current user");
        }

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new IllegalStateException("Order is not delivered yet");
        }

        Long productExternalId = product.getExternalId();
        if (productExternalId == null) {
            throw new IllegalStateException("Product has no external id, cannot be reviewed");
        }

        boolean productInOrder = order.getItems().stream()
                .anyMatch(item -> {
                    Long nhanhId = safeParseLong(item.getNhanhProductId());
                    return nhanhId != null && nhanhId.equals(productExternalId);
                });

        if (!productInOrder) {
            throw new IllegalStateException("Product not found in order");
        }

        if (reviewRepository.existsByAccountIdAndProductId(account.getId(), product.getId())) {
            throw new IllegalStateException("You have already reviewed this product");
        }

        List<String> imageUrls = Optional.ofNullable(request.getImageUrls())
                .orElse(Collections.emptyList())
                .stream()
                .filter(url -> url != null && !url.isBlank())
                .map(String::trim)
                .toList();

        String imageUrlsJson;
        try {
            imageUrlsJson = objectMapper.writeValueAsString(imageUrls);
        } catch (Exception e) {
            imageUrlsJson = "[]";
        }

        Review review = Review.builder()
                .account(account)
                .product(product)
                .order(order)
                .rating(request.getRating())
                .content(request.getContent().trim())
                .imageUrls(imageUrlsJson)
                .status(ReviewStatus.PUBLISHED)
                .build();

        review = reviewRepository.save(review);
        return toResponseDto(review);
    }

    @Transactional(readOnly = true)
    public ReviewEligibilityResponse getReviewEligibility(Long productId, String accountEmail) {
        Account account = resolveAccount(accountEmail);

        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + productId));

        if (reviewRepository.existsByAccountIdAndProductId(account.getId(), product.getId())) {
            return ReviewEligibilityResponse.builder()
                    .canReview(false)
                    .reason("Bạn đã đánh giá sản phẩm này.")
                    .alreadyReviewed(true)
                    .deliveredOrderFound(false)
                    .build();
        }

        Long productExternalId = product.getExternalId();
        if (productExternalId == null) {
            return ReviewEligibilityResponse.builder()
                    .canReview(false)
                    .reason("Sản phẩm chưa có mã đồng bộ để xác minh đơn hàng.")
                    .alreadyReviewed(false)
                    .deliveredOrderFound(false)
                    .build();
        }

        List<Order> deliveredOrders = orderRepository.findDeliveredCustomerOrdersForReview(account.getEmail().trim());
        Optional<Order> matchingOrder = deliveredOrders.stream()
                .filter(order -> order.getItems() != null)
                .filter(order -> order.getItems().stream().anyMatch(item -> itemMatchesProduct(item, productExternalId)))
                .findFirst();

        if (matchingOrder.isEmpty()) {
            return ReviewEligibilityResponse.builder()
                    .canReview(false)
                    .reason("Hãy mua hàng rồi mới đăng review.")
                    .alreadyReviewed(false)
                    .deliveredOrderFound(false)
                    .build();
        }

        return ReviewEligibilityResponse.builder()
                .canReview(true)
                .reason("Đơn hàng đã giao hợp lệ. Bạn có thể gửi đánh giá cho sản phẩm này.")
                .orderId(matchingOrder.get().getId())
                .alreadyReviewed(false)
                .deliveredOrderFound(true)
                .build();
    }

    @Transactional
    public ReviewResponseDto updateReviewStatus(Long reviewId, UpdateReviewStatusRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review not found with id: " + reviewId));
        review.setStatus(request.getStatus());
        review = reviewRepository.save(review);
        return toResponseDto(review);
    }

    @Transactional
    public ReviewResponseDto replyToReview(Long reviewId, ReplyReviewRequest request, String adminEmail) {
        Account admin = accountRepo.findByEmail(adminEmail)
                .orElseThrow(() -> new ForbiddenOperationException("Admin account not found"));

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review not found with id: " + reviewId));

        review.setAdminReply(request.getAdminReply().trim());
        review.setRepliedBy(admin);
        review.setRepliedAt(LocalDateTime.now());
        review = reviewRepository.save(review);
        return toResponseDto(review);
    }

    @Transactional
    public void deleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review not found with id: " + reviewId));
        reviewRepository.delete(review);
    }

    public Page<ReviewResponseDto> getPublicReviews(Long productId, Pageable pageable) {
        return reviewRepository.findByProductIdAndStatus(productId, ReviewStatus.PUBLISHED, pageable)
                .map(this::toResponseDto);
    }

    public Page<ReviewResponseDto> getLatestPublicReviews(Pageable pageable) {
        return reviewRepository.findByStatus(ReviewStatus.PUBLISHED, pageable)
                .map(this::toResponseDto);
    }

    public Page<ReviewResponseDto> getAdminReviews(ReviewStatus status, Pageable pageable) {
        if (status == null) {
            return reviewRepository.findAll(pageable).map(this::toResponseDto);
        }
        return reviewRepository.findByStatus(status, pageable).map(this::toResponseDto);
    }

    public ReviewResponseDto getReviewById(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review not found with id: " + reviewId));
        return toResponseDto(review);
    }

    public ReviewResponseDto toResponseDto(Review review) {
        List<String> imageUrls = parseImageUrls(review.getImageUrls());

        return ReviewResponseDto.builder()
                .id(review.getId())
                .productId(review.getProduct().getId())
                .orderId(review.getOrder().getId())
                .rating(review.getRating())
                .content(review.getContent())
                .imageUrls(imageUrls)
                .status(review.getStatus())
                .customerName(review.getAccount().getFullName())
                .adminReply(review.getAdminReply())
                .repliedAt(review.getRepliedAt())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }

    private List<String> parseImageUrls(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private Account resolveAccount(String accountEmail) {
        if (accountEmail == null || accountEmail.isBlank()) {
            throw new ForbiddenOperationException("Authentication is required");
        }
        return accountRepo.findByEmail(accountEmail)
                .orElseThrow(() -> new ForbiddenOperationException("Account not found"));
    }

    private boolean itemMatchesProduct(OrderItem item, Long productExternalId) {
        if (item == null || productExternalId == null) {
            return false;
        }
        Long nhanhId = safeParseLong(item.getNhanhProductId());
        return nhanhId != null && nhanhId.equals(productExternalId);
    }

    static Long safeParseLong(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
