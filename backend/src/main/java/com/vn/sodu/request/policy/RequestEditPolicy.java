package com.vn.sodu.request.policy;

import com.vn.sodu.request.RequestStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class RequestEditPolicy {

    private static final Map<RequestStatus, Set<RequestEditableField>> MATRIX = buildMatrix();

    public boolean canEdit(RequestStatus status, RequestEditableField field) {
        if (status == null || field == null) {
            return false;
        }
        return MATRIX.getOrDefault(status, EnumSet.noneOf(RequestEditableField.class)).contains(field);
    }

    public boolean canEditItems(RequestStatus status) {
        return canEdit(status, RequestEditableField.ITEMS);
    }

    public boolean canEditItemQuantity(RequestStatus status) {
        return canEdit(status, RequestEditableField.ITEM_QUANTITY);
    }

    public boolean canEditImages(RequestStatus status) {
        return canEdit(status, RequestEditableField.IMAGES);
    }

    public boolean canEditRequirements(RequestStatus status) {
        return canEdit(status, RequestEditableField.REQUIREMENTS);
    }

    public boolean canEditCustomerPhone(RequestStatus status) {
        return canEdit(status, RequestEditableField.CUSTOMER_PHONE);
    }

    public boolean canEditType(RequestStatus status) {
        return canEdit(status, RequestEditableField.TYPE);
    }

    public Set<RequestEditableField> editableFields(RequestStatus status) {
        if (status == null) {
            return Set.of();
        }
        return Set.copyOf(MATRIX.getOrDefault(status, EnumSet.noneOf(RequestEditableField.class)));
    }

    private static Map<RequestStatus, Set<RequestEditableField>> buildMatrix() {
        EnumMap<RequestStatus, Set<RequestEditableField>> matrix = new EnumMap<>(RequestStatus.class);

        matrix.put(RequestStatus.PENDING, EnumSet.allOf(RequestEditableField.class));

        matrix.put(RequestStatus.REVIEWING, EnumSet.of(
                RequestEditableField.ITEMS,
                RequestEditableField.ITEM_QUANTITY,
                RequestEditableField.IMAGES,
                RequestEditableField.REQUIREMENTS,
                RequestEditableField.CUSTOMER_PHONE
        ));

        matrix.put(RequestStatus.SOURCING, EnumSet.of(
                RequestEditableField.ITEMS,
                RequestEditableField.IMAGES,
                RequestEditableField.REQUIREMENTS
        ));

        matrix.put(RequestStatus.WAITING_CUSTOMER, EnumSet.of(
                RequestEditableField.ITEMS,
                RequestEditableField.IMAGES,
                RequestEditableField.REQUIREMENTS
        ));

        matrix.put(RequestStatus.APPROVED, EnumSet.noneOf(RequestEditableField.class));
        matrix.put(RequestStatus.REJECTED, EnumSet.noneOf(RequestEditableField.class));
        matrix.put(RequestStatus.CANCELLED, EnumSet.noneOf(RequestEditableField.class));

        return Map.copyOf(matrix);
    }
}
