package com.vn.sodu.request.policy;

import com.vn.sodu.request.RequestStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RequestEditPolicyTest {

    private final RequestEditPolicy policy = new RequestEditPolicy();

    @Test
    void keepsPendingFullyEditable() {
        assertThat(policy.canEditType(RequestStatus.PENDING)).isTrue();
        assertThat(policy.canEditCustomerPhone(RequestStatus.PENDING)).isTrue();
        assertThat(policy.canEditItems(RequestStatus.PENDING)).isTrue();
        assertThat(policy.canEditItemQuantity(RequestStatus.PENDING)).isTrue();
        assertThat(policy.canEditImages(RequestStatus.PENDING)).isTrue();
        assertThat(policy.canEditRequirements(RequestStatus.PENDING)).isTrue();
    }

    @Test
    void locksDownSensitiveFieldsAfterReviewing() {
        assertThat(policy.canEditType(RequestStatus.REVIEWING)).isFalse();
        assertThat(policy.canEditCustomerPhone(RequestStatus.REVIEWING)).isTrue();
        assertThat(policy.canEditItems(RequestStatus.REVIEWING)).isTrue();
        assertThat(policy.canEditItemQuantity(RequestStatus.REVIEWING)).isTrue();
        assertThat(policy.canEditImages(RequestStatus.REVIEWING)).isTrue();
        assertThat(policy.canEditRequirements(RequestStatus.REVIEWING)).isTrue();
    }

    @Test
    void keepsSourcingAndWaitingCustomerPartialOnly() {
        assertThat(policy.canEditItems(RequestStatus.SOURCING)).isTrue();
        assertThat(policy.canEditItemQuantity(RequestStatus.SOURCING)).isFalse();
        assertThat(policy.canEditImages(RequestStatus.SOURCING)).isTrue();
        assertThat(policy.canEditRequirements(RequestStatus.SOURCING)).isTrue();

        assertThat(policy.canEditItems(RequestStatus.WAITING_CUSTOMER)).isTrue();
        assertThat(policy.canEditItemQuantity(RequestStatus.WAITING_CUSTOMER)).isFalse();
        assertThat(policy.canEditImages(RequestStatus.WAITING_CUSTOMER)).isTrue();
        assertThat(policy.canEditRequirements(RequestStatus.WAITING_CUSTOMER)).isTrue();
    }

    @Test
    void approvedRejectedAndCancelledAreImmutable() {
        assertThat(policy.editableFields(RequestStatus.APPROVED)).isEmpty();
        assertThat(policy.editableFields(RequestStatus.REJECTED)).isEmpty();
        assertThat(policy.editableFields(RequestStatus.CANCELLED)).isEmpty();
    }
}
