import { RequestStatus, RequestType } from '../enum/union-types';

export type RequestActor = 'CUSTOMER' | 'ADMIN' | 'SYSTEM';

export interface RequestWorkflowStep {
    status: RequestStatus;
    label: string;
    actor: RequestActor;
}

export interface RequestWorkflowView {
    label: string;
    description: string;
    actor: RequestActor;
    tone: string;
}

export interface AdminPrimaryAction {
    targetStatus: RequestStatus;
    label: string;
    kind: 'RECEIVE' | 'SEND_QUOTE' | 'APPROVE';
    requiresDeposit?: boolean;
}

export const OPEN_REQUEST_STATUSES: RequestStatus[] = [
    'PENDING',
    'REVIEWING',
    'SOURCING',
    'WAITING_CUSTOMER'
];

export const isRequestOpen = (status: RequestStatus): boolean =>
    OPEN_REQUEST_STATUSES.includes(status);

export const canCustomerEditRequest = (status: RequestStatus): boolean => status === 'PENDING';

export const REQUEST_STATUS_VIEWS: Record<RequestStatus, RequestWorkflowView> = {
    PENDING: {
        label: 'Chờ tiếp nhận',
        description: 'SOBU sẽ tiếp nhận và phân loại yêu cầu của bạn.',
        actor: 'ADMIN',
        tone: 'bg-amber-50 text-amber-700 border-amber-200'
    },
    REVIEWING: {
        label: 'Đang đánh giá',
        description: 'SOBU đang kiểm tra tính khả thi, chi phí hoặc suất đặt hàng.',
        actor: 'ADMIN',
        tone: 'bg-blue-50 text-blue-700 border-blue-200'
    },
    SOURCING: {
        label: 'Đang tìm nguồn',
        description: 'SOBU đang liên hệ nguồn hàng và xác minh thông tin sản phẩm.',
        actor: 'ADMIN',
        tone: 'bg-purple-50 text-purple-700 border-purple-200'
    },
    WAITING_CUSTOMER: {
        label: 'Chờ khách xác nhận',
        description: 'Báo giá đã sẵn sàng. Vui lòng kiểm tra và liên hệ SOBU để xác nhận.',
        actor: 'CUSTOMER',
        tone: 'bg-indigo-50 text-indigo-700 border-indigo-200'
    },
    APPROVED: {
        label: 'Đã duyệt',
        description: 'Yêu cầu đã được chốt và chuyển thành đơn hàng.',
        actor: 'CUSTOMER',
        tone: 'bg-emerald-50 text-emerald-700 border-emerald-200'
    },
    REJECTED: {
        label: 'Từ chối',
        description: 'SOBU chưa thể đáp ứng yêu cầu này.',
        actor: 'SYSTEM',
        tone: 'bg-red-50 text-red-700 border-red-200'
    },
    CANCELLED: {
        label: 'Đã hủy',
        description: 'Yêu cầu đã được hủy và không còn xử lý.',
        actor: 'SYSTEM',
        tone: 'bg-gray-50 text-gray-700 border-gray-200'
    }
};

const WORKFLOW_STEPS: Record<'CUSTOM' | 'FINDING' | 'PREORDER', RequestWorkflowStep[]> = {
    CUSTOM: [
        { status: 'PENDING', label: 'Tiếp nhận', actor: 'ADMIN' },
        { status: 'REVIEWING', label: 'Đánh giá & báo giá', actor: 'ADMIN' },
        { status: 'WAITING_CUSTOMER', label: 'Khách xác nhận', actor: 'CUSTOMER' },
        { status: 'APPROVED', label: 'Duyệt & đặt cọc', actor: 'CUSTOMER' }
    ],
    FINDING: [
        { status: 'PENDING', label: 'Tiếp nhận', actor: 'ADMIN' },
        { status: 'SOURCING', label: 'Tìm nguồn hàng', actor: 'ADMIN' },
        { status: 'WAITING_CUSTOMER', label: 'Khách xác nhận', actor: 'CUSTOMER' },
        { status: 'APPROVED', label: 'Duyệt & thanh toán', actor: 'CUSTOMER' }
    ],
    PREORDER: [
        { status: 'PENDING', label: 'Tiếp nhận', actor: 'ADMIN' },
        { status: 'REVIEWING', label: 'Kiểm tra suất', actor: 'ADMIN' },
        { status: 'APPROVED', label: 'Duyệt & đặt cọc', actor: 'CUSTOMER' }
    ]
};

export const getRequestWorkflowSteps = (type: RequestType): RequestWorkflowStep[] =>
    type === 'NORMAL' ? [] : WORKFLOW_STEPS[type];

export const getRequestWorkflowStepIndex = (type: RequestType, status: RequestStatus): number => {
    const steps = getRequestWorkflowSteps(type);
    const exactIndex = steps.findIndex((step) => step.status === status);
    if (exactIndex >= 0) return exactIndex;

    // Backend currently creates PREORDER in SOURCING; display it as the slot-review step.
    if (type === 'PREORDER' && status === 'SOURCING') return 1;
    return status === 'REJECTED' || status === 'CANCELLED' ? -1 : 0;
};

export const getAdminPrimaryAction = (
    type: RequestType,
    status: RequestStatus
): AdminPrimaryAction | null => {
    if (status === 'PENDING') {
        return type === 'FINDING'
            ? { targetStatus: 'SOURCING', label: 'Bắt đầu tìm nguồn', kind: 'RECEIVE' }
            : { targetStatus: 'REVIEWING', label: type === 'PREORDER' ? 'Kiểm tra suất đặt' : 'Tiếp nhận đánh giá', kind: 'RECEIVE' };
    }
    if (type === 'CUSTOM' && status === 'REVIEWING') {
        return { targetStatus: 'WAITING_CUSTOMER', label: 'Gửi báo giá cho khách', kind: 'SEND_QUOTE' };
    }
    if (type === 'FINDING' && status === 'REVIEWING') {
        return { targetStatus: 'SOURCING', label: 'Bắt đầu tìm nguồn', kind: 'RECEIVE' };
    }
    if (type === 'FINDING' && status === 'SOURCING') {
        return { targetStatus: 'WAITING_CUSTOMER', label: 'Gửi thông tin & báo giá', kind: 'SEND_QUOTE' };
    }
    if (type === 'PREORDER' && (status === 'REVIEWING' || status === 'SOURCING')) {
        return { targetStatus: 'APPROVED', label: 'Xác nhận còn suất & duyệt', kind: 'APPROVE', requiresDeposit: true };
    }
    if ((type === 'CUSTOM' || type === 'FINDING') && status === 'WAITING_CUSTOMER') {
        return { targetStatus: 'APPROVED', label: 'Ghi nhận khách xác nhận & duyệt', kind: 'APPROVE', requiresDeposit: true };
    }
    return null;
};

export const canAdminEditQuotation = (type: RequestType, status: RequestStatus): boolean =>
    (type === 'CUSTOM' && status === 'REVIEWING') ||
    (type === 'FINDING' && status === 'SOURCING');

export const getAllowedRequestTransitions = (
    status: RequestStatus,
    type: RequestType
): RequestStatus[] => {
    const primary = getAdminPrimaryAction(type, status);
    if (status === 'APPROVED') return ['CANCELLED'];
    if (!isRequestOpen(status)) return [];
    return [
        ...(primary ? [primary.targetStatus] : []),
        'REJECTED',
        'CANCELLED'
    ];
};
