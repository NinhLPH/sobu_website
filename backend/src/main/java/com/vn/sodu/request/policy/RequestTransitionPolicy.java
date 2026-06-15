package com.vn.sodu.request.policy;

import com.vn.sodu.request.RequestStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class RequestTransitionPolicy {

    private static final Map<RequestStatus, Set<RequestStatus>> MATRIX = buildMatrix();

    public boolean canTransition(RequestStatus from, RequestStatus to) {
        if (from == null || to == null) {
            return false;
        }
        return MATRIX.getOrDefault(from, EnumSet.noneOf(RequestStatus.class)).contains(to);
    }

    public void validateTransition(RequestStatus from, RequestStatus to) {
        if (!canTransition(from, to)) {
            throw new IllegalStateException("Invalid request transition from " + from + " to " + to);
        }
    }

    public Set<RequestStatus> allowedTransitions(RequestStatus from) {
        if (from == null) {
            return Set.of();
        }
        return Set.copyOf(MATRIX.getOrDefault(from, EnumSet.noneOf(RequestStatus.class)));
    }

    private static Map<RequestStatus, Set<RequestStatus>> buildMatrix() {
        EnumMap<RequestStatus, Set<RequestStatus>> matrix = new EnumMap<>(RequestStatus.class);

        matrix.put(RequestStatus.PENDING, EnumSet.of(
                RequestStatus.REVIEWING,
                RequestStatus.SOURCING,
                RequestStatus.REJECTED,
                RequestStatus.CANCELLED
        ));

        matrix.put(RequestStatus.REVIEWING, EnumSet.of(
                RequestStatus.SOURCING,
                RequestStatus.WAITING_CUSTOMER,
                RequestStatus.APPROVED,
                RequestStatus.REJECTED,
                RequestStatus.CANCELLED
        ));

        matrix.put(RequestStatus.SOURCING, EnumSet.of(
                RequestStatus.REVIEWING,
                RequestStatus.WAITING_CUSTOMER,
                RequestStatus.APPROVED,
                RequestStatus.REJECTED,
                RequestStatus.CANCELLED
        ));

        matrix.put(RequestStatus.WAITING_CUSTOMER, EnumSet.of(
                RequestStatus.REVIEWING,
                RequestStatus.SOURCING,
                RequestStatus.APPROVED,
                RequestStatus.REJECTED,
                RequestStatus.CANCELLED
        ));

        matrix.put(RequestStatus.APPROVED, EnumSet.of(RequestStatus.CANCELLED));
        matrix.put(RequestStatus.REJECTED, EnumSet.noneOf(RequestStatus.class));
        matrix.put(RequestStatus.CANCELLED, EnumSet.noneOf(RequestStatus.class));

        return Map.copyOf(matrix);
    }
}
