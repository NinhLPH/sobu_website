package com.vn.sodu.review;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.sodu.global.exception.ForbiddenOperationException;
import com.vn.sodu.global.exception.NotFoundException;
import com.vn.sodu.order.Order;
import com.vn.sodu.order.OrderItem;
import com.vn.sodu.order.OrderStatus;
import com.vn.sodu.order.repo.OrderRepository;
import com.vn.sodu.product.Product;
import com.vn.sodu.product.repo.ProductRepo;
import com.vn.sodu.review.dto.CreateReviewRequest;
import com.vn.sodu.review.dto.ReplyReviewRequest;
import com.vn.sodu.review.dto.ReviewEligibilityResponse;
import com.vn.sodu.review.dto.ReviewResponseDto;
import com.vn.sodu.review.dto.UpdateReviewStatusRequest;
import com.vn.sodu.user.Account;
import com.vn.sodu.user.AccountRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ProductRepo productRepo;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private AccountRepo accountRepo;

    private ObjectMapper objectMapper;

    private ReviewService reviewService;

    private Account account;
    private Product product;
    private Order order;
    private OrderItem orderItem;
    private CreateReviewRequest validRequest;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        reviewService = new ReviewService(reviewRepository, productRepo, orderRepository, accountRepo, objectMapper);

        account = Account.builder()
                .id(1L)
                .email("customer@example.com")
                .fullName("Nguyen Van A")
                .build();

        product = Product.builder()
                .id(100L)
                .externalId(500L)
                .build();

        orderItem = OrderItem.builder()
                .id(10L)
                .nhanhProductId("500")
                .build();

        order = Order.builder()
                .id(200L)
                .customerEmail("customer@example.com")
                .status(OrderStatus.DELIVERED)
                .items(List.of(orderItem))
                .build();

        validRequest = new CreateReviewRequest();
        validRequest.setProductId(100L);
        validRequest.setOrderId(200L);
        validRequest.setRating(5);
        validRequest.setContent("Great product!");
        validRequest.setImageUrls(List.of("https://example.com/img1.jpg"));
    }

    @Test
    void createReviewSuccessfully() {
        when(accountRepo.findByEmail("customer@example.com")).thenReturn(Optional.of(account));
        when(productRepo.findById(100L)).thenReturn(Optional.of(product));
        when(orderRepository.findWithItemsAndRequestById(200L)).thenReturn(Optional.of(order));
        when(reviewRepository.existsByAccountIdAndProductId(1L, 100L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review r = invocation.getArgument(0);
            return Review.builder()
                    .id(1L)
                    .account(r.getAccount())
                    .product(r.getProduct())
                    .order(r.getOrder())
                    .rating(r.getRating())
                    .content(r.getContent())
                    .imageUrls(r.getImageUrls())
                    .status(ReviewStatus.PUBLISHED)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
        });

        ReviewResponseDto result = reviewService.createReview(validRequest, "customer@example.com");

        assertThat(result).isNotNull();
        assertThat(result.getRating()).isEqualTo(5);
        assertThat(result.getContent()).isEqualTo("Great product!");
        assertThat(result.getStatus()).isEqualTo(ReviewStatus.PUBLISHED);
        assertThat(result.getCustomerName()).isEqualTo("Nguyen Van A");
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void createReviewRejectsUnauthenticatedAccount() {
        when(accountRepo.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.createReview(validRequest, "unknown@example.com"))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("Account not found");
    }

    @Test
    void createReviewRejectsNonExistentProduct() {
        when(accountRepo.findByEmail("customer@example.com")).thenReturn(Optional.of(account));
        when(productRepo.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.createReview(validRequest, "customer@example.com"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    void createReviewRejectsNonExistentOrder() {
        when(accountRepo.findByEmail("customer@example.com")).thenReturn(Optional.of(account));
        when(productRepo.findById(100L)).thenReturn(Optional.of(product));
        when(orderRepository.findWithItemsAndRequestById(200L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.createReview(validRequest, "customer@example.com"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    void createReviewRejectsOrderNotBelongingToCustomer() {
        when(accountRepo.findByEmail("customer@example.com")).thenReturn(Optional.of(account));
        when(productRepo.findById(100L)).thenReturn(Optional.of(product));
        order.setCustomerEmail("other@example.com");
        when(orderRepository.findWithItemsAndRequestById(200L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> reviewService.createReview(validRequest, "customer@example.com"))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void createReviewRejectsNonDeliveredOrder() {
        when(accountRepo.findByEmail("customer@example.com")).thenReturn(Optional.of(account));
        when(productRepo.findById(100L)).thenReturn(Optional.of(product));
        order.setStatus(OrderStatus.PROCESSING);
        when(orderRepository.findWithItemsAndRequestById(200L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> reviewService.createReview(validRequest, "customer@example.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not delivered");
    }

    @Test
    void createReviewRejectsProductWithoutExternalId() {
        when(accountRepo.findByEmail("customer@example.com")).thenReturn(Optional.of(account));
        product.setExternalId(null);
        when(productRepo.findById(100L)).thenReturn(Optional.of(product));
        when(orderRepository.findWithItemsAndRequestById(200L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> reviewService.createReview(validRequest, "customer@example.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no external id");
    }

    @Test
    void createReviewRejectsProductNotInOrder() {
        when(accountRepo.findByEmail("customer@example.com")).thenReturn(Optional.of(account));
        when(productRepo.findById(100L)).thenReturn(Optional.of(product));
        orderItem.setNhanhProductId("999");
        when(orderRepository.findWithItemsAndRequestById(200L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> reviewService.createReview(validRequest, "customer@example.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Product not found in order");
    }

    @Test
    void createReviewRejectsDuplicate() {
        when(accountRepo.findByEmail("customer@example.com")).thenReturn(Optional.of(account));
        when(productRepo.findById(100L)).thenReturn(Optional.of(product));
        when(orderRepository.findWithItemsAndRequestById(200L)).thenReturn(Optional.of(order));
        when(reviewRepository.existsByAccountIdAndProductId(1L, 100L)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.createReview(validRequest, "customer@example.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already reviewed");
    }

    @Test
    void reviewEligibilityRejectsMissingAuthentication() {
        assertThatThrownBy(() -> reviewService.getReviewEligibility(100L, null))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("Authentication is required");
    }

    @Test
    void reviewEligibilityRejectsMissingAccount() {
        when(accountRepo.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.getReviewEligibility(100L, "unknown@example.com"))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("Account not found");
    }

    @Test
    void reviewEligibilityRejectsMissingProduct() {
        when(accountRepo.findByEmail("customer@example.com")).thenReturn(Optional.of(account));
        when(productRepo.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.getReviewEligibility(999L, "customer@example.com"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    void reviewEligibilityRejectsProductWithoutExternalId() {
        product.setExternalId(null);
        when(accountRepo.findByEmail("customer@example.com")).thenReturn(Optional.of(account));
        when(productRepo.findById(100L)).thenReturn(Optional.of(product));
        when(reviewRepository.existsByAccountIdAndProductId(1L, 100L)).thenReturn(false);

        ReviewEligibilityResponse result = reviewService.getReviewEligibility(100L, "customer@example.com");

        assertThat(result.isCanReview()).isFalse();
        assertThat(result.isAlreadyReviewed()).isFalse();
        assertThat(result.isDeliveredOrderFound()).isFalse();
        assertThat(result.getReason()).contains("mã đồng bộ");
    }

    @Test
    void reviewEligibilityReturnsAlreadyReviewed() {
        when(accountRepo.findByEmail("customer@example.com")).thenReturn(Optional.of(account));
        when(productRepo.findById(100L)).thenReturn(Optional.of(product));
        when(reviewRepository.existsByAccountIdAndProductId(1L, 100L)).thenReturn(true);

        ReviewEligibilityResponse result = reviewService.getReviewEligibility(100L, "customer@example.com");

        assertThat(result.isCanReview()).isFalse();
        assertThat(result.isAlreadyReviewed()).isTrue();
        assertThat(result.getReason()).contains("đã đánh giá");
    }

    @Test
    void reviewEligibilityRejectsWhenNoDeliveredMatchingOrderExists() {
        when(accountRepo.findByEmail("customer@example.com")).thenReturn(Optional.of(account));
        when(productRepo.findById(100L)).thenReturn(Optional.of(product));
        when(reviewRepository.existsByAccountIdAndProductId(1L, 100L)).thenReturn(false);
        when(orderRepository.findDeliveredCustomerOrdersForReview("customer@example.com")).thenReturn(List.of());

        ReviewEligibilityResponse result = reviewService.getReviewEligibility(100L, "customer@example.com");

        assertThat(result.isCanReview()).isFalse();
        assertThat(result.isDeliveredOrderFound()).isFalse();
        assertThat(result.getReason()).contains("mua hàng");
    }

    @Test
    void reviewEligibilityAllowsDeliveredOrderWithMatchingProduct() {
        when(accountRepo.findByEmail("customer@example.com")).thenReturn(Optional.of(account));
        when(productRepo.findById(100L)).thenReturn(Optional.of(product));
        when(reviewRepository.existsByAccountIdAndProductId(1L, 100L)).thenReturn(false);
        when(orderRepository.findDeliveredCustomerOrdersForReview("customer@example.com")).thenReturn(List.of(order));

        ReviewEligibilityResponse result = reviewService.getReviewEligibility(100L, "customer@example.com");

        assertThat(result.isCanReview()).isTrue();
        assertThat(result.isDeliveredOrderFound()).isTrue();
        assertThat(result.getOrderId()).isEqualTo(200L);
    }

    @Test
    void createReviewNormalizesImageUrls() {
        when(accountRepo.findByEmail("customer@example.com")).thenReturn(Optional.of(account));
        when(productRepo.findById(100L)).thenReturn(Optional.of(product));
        when(orderRepository.findWithItemsAndRequestById(200L)).thenReturn(Optional.of(order));
        when(reviewRepository.existsByAccountIdAndProductId(1L, 100L)).thenReturn(false);

        java.util.List<String> urls = new java.util.ArrayList<>();
        urls.add("  https://example.com/img.jpg  ");
        urls.add("");
        urls.add(null);
        urls.add("https://example.com/img2.jpg");
        validRequest.setImageUrls(urls);

        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review r = invocation.getArgument(0);
            return Review.builder()
                    .id(1L)
                    .account(r.getAccount())
                    .product(r.getProduct())
                    .order(r.getOrder())
                    .rating(r.getRating())
                    .content(r.getContent())
                    .imageUrls(r.getImageUrls())
                    .status(ReviewStatus.PUBLISHED)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
        });

        ReviewResponseDto result = reviewService.createReview(validRequest, "customer@example.com");

        assertThat(result).isNotNull();
        assertThat(result.getImageUrls()).hasSize(2);
        assertThat(result.getImageUrls().get(0)).isEqualTo("https://example.com/img.jpg");
        assertThat(result.getImageUrls().get(1)).isEqualTo("https://example.com/img2.jpg");
    }

    @Test
    void updateReviewStatusToHidden() {
        Review review = Review.builder()
                .id(1L)
                .account(account)
                .product(product)
                .order(order)
                .rating(5)
                .content("Great!")
                .status(ReviewStatus.PUBLISHED)
                .build();

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateReviewStatusRequest req = new UpdateReviewStatusRequest();
        req.setStatus(ReviewStatus.HIDDEN);

        ReviewResponseDto result = reviewService.updateReviewStatus(1L, req);

        assertThat(result.getStatus()).isEqualTo(ReviewStatus.HIDDEN);
        verify(reviewRepository).save(review);
    }

    @Test
    void updateReviewStatusThrowsWhenNotFound() {
        when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

        UpdateReviewStatusRequest req = new UpdateReviewStatusRequest();
        req.setStatus(ReviewStatus.HIDDEN);

        assertThatThrownBy(() -> reviewService.updateReviewStatus(999L, req))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void replyToReviewSetsAdminReplyFields() {
        Account admin = Account.builder().id(2L).email("admin@example.com").fullName("Admin").build();
        Review review = Review.builder()
                .id(1L)
                .account(account)
                .product(product)
                .order(order)
                .rating(5)
                .content("Great!")
                .status(ReviewStatus.PUBLISHED)
                .build();

        when(accountRepo.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReplyReviewRequest req = new ReplyReviewRequest();
        req.setAdminReply("Thank you for your review!");

        ReviewResponseDto result = reviewService.replyToReview(1L, req, "admin@example.com");

        assertThat(result.getAdminReply()).isEqualTo("Thank you for your review!");
        assertThat(result.getRepliedAt()).isNotNull();
        verify(reviewRepository).save(review);
    }

    @Test
    void deleteReviewRemovesReview() {
        Review review = Review.builder().id(1L).build();
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        reviewService.deleteReview(1L);

        verify(reviewRepository).delete(review);
    }

    @Test
    void deleteReviewThrowsWhenNotFound() {
        when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.deleteReview(999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void safeParseLongParsesNumericString() {
        assertThat(ReviewService.safeParseLong("500")).isEqualTo(500L);
        assertThat(ReviewService.safeParseLong("0")).isEqualTo(0L);
        assertThat(ReviewService.safeParseLong("-1")).isEqualTo(-1L);
    }

    @Test
    void safeParseLongReturnsNullForInvalidInput() {
        assertThat(ReviewService.safeParseLong(null)).isNull();
        assertThat(ReviewService.safeParseLong("")).isNull();
        assertThat(ReviewService.safeParseLong("  ")).isNull();
        assertThat(ReviewService.safeParseLong("abc")).isNull();
        assertThat(ReviewService.safeParseLong("12.5")).isNull();
    }
}
