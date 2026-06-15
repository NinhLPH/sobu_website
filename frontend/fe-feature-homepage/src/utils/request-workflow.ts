import { RequestStatus, RequestType } from '../enum/union-types';

export const OPEN_REQUEST_STATUSES: RequestStatus[] = [
    'PENDING',
    'REVIEWING',
    'SOURCING',
    'WAITING_CUSTOMER'
];

export const isRequestOpen = (status: RequestStatus): boolean =>
    OPEN_REQUEST_STATUSES.includes(status);

export const getAllowedRequestTransitions = (
    status: RequestStatus,
    type: RequestType
): RequestStatus[] => {
    if (status === 'PENDING') {
        return [
            type === 'FINDING' ? 'SOURCING' : 'REVIEWING',
            'REJECTED',
            'CANCELLED'
        ];
    }

    if (status === 'REVIEWING') {
        return ['SOURCING', 'WAITING_CUSTOMER', 'APPROVED', 'REJECTED', 'CANCELLED'];
    }

    if (status === 'SOURCING') {
        return ['REVIEWING', 'WAITING_CUSTOMER', 'APPROVED', 'REJECTED', 'CANCELLED'];
    }

    if (status === 'WAITING_CUSTOMER') {
        return ['REVIEWING', 'SOURCING', 'APPROVED', 'REJECTED', 'CANCELLED'];
    }

    if (status === 'APPROVED') {
        return ['CANCELLED'];
    }

    return [];
};
