import {OrderResponseDto} from '../interface/order.model';

const HISTORICAL_SYNC_STATUSES = new Set([
    'SYNCED',
    'FAILED',
    'NEED_RECONCILE',
    'DEAD'
]);

export const hasNhanhHistory = (order?: OrderResponseDto | null): boolean => {
    if (!order) {
        return false;
    }

    return Boolean(
        order.nhanhOrderId
        || order.nhanhOrderCode
        || order.lastSyncAt
        || order.lastSyncMessage
        || order.syncError
        || (order.nhanhSyncStage && order.nhanhSyncStage !== 'NONE')
        || (order.syncStatus && HISTORICAL_SYNC_STATUSES.has(order.syncStatus))
    );
};

export const getNhanhSyncStatusStyle = (status?: string): string => {
    switch (status) {
        case 'SYNCED':
            return 'border-green-200 bg-green-50 text-green-700';
        case 'FAILED':
            return 'border-red-200 bg-red-50 text-red-700';
        case 'NEED_RECONCILE':
            return 'border-orange-200 bg-orange-50 text-orange-700';
        case 'DEAD':
            return 'border-slate-300 bg-slate-100 text-slate-700';
        case 'PENDING':
            return 'border-amber-200 bg-amber-50 text-amber-700';
        default:
            return 'border-gray-200 bg-gray-50 text-gray-700';
    }
};

export const getNhanhSyncStatusText = (status?: string): string => {
    switch (status) {
        case 'SYNCED':
            return 'Đã đồng bộ';
        case 'FAILED':
            return 'Thất bại';
        case 'NEED_RECONCILE':
            return 'Cần đối soát';
        case 'DEAD':
            return 'Đã dừng retry';
        case 'PENDING':
            return 'Đang chờ';
        default:
            return status || 'Chưa cập nhật';
    }
};

const getHistoryTimestamp = (order: OrderResponseDto): number => {
    const value = order.lastSyncAt || order.updatedAt || order.createdAt;
    const timestamp = value ? Date.parse(value) : 0;
    return Number.isFinite(timestamp) ? timestamp : 0;
};

export const compareNhanhHistoryNewestFirst = (
    first: OrderResponseDto,
    second: OrderResponseDto
): number => getHistoryTimestamp(second) - getHistoryTimestamp(first);
