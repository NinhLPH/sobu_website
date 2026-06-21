export type RequestType = 'NORMAL' | 'PREORDER' | 'FINDING' | 'CUSTOM';
export type RequestStatus = 'PENDING' | 'REVIEWING' | 'SOURCING' | 'WAITING_CUSTOMER' | 'APPROVED' | 'REJECTED' | 'CANCELLED';
export type OrderStatus =
    'PENDING'
    | 'NEW'
    | 'WAITING_DEPOSIT'
    | 'DEPOSIT_PAID'
    | 'READY_FOR_FINAL_PAYMENT'
    | 'PROCESSING'
    | 'SHIPPED'
    | 'DELIVERED'
    | 'CANCELLED';
export type OrderSyncStatus = 'PENDING' | 'SYNCED' | 'FAILED';
export type NhanhSyncStage =
    'NONE'
    | 'NORMAL_ORDER_CREATED'
    | 'PREORDER_DEPOSIT_CREATED'
    | 'PREORDER_FINAL_UPDATED';
export type PaymentStatus = 'PENDING' | 'PAID' | 'FAILED' | 'CANCELLED' | 'EXPIRED' | 'REFUNDED';
export type PaymentMethod = 'ONLINE' | 'COD';
export type AccountStatus = 'ACTIVE' | 'INACTIVE' | 'BANNED';
export type DeviceType = 'WEB' | 'MOBILE' | 'ALL';
export type BannerPosition = 'HOME_TOP' | 'HOME_MIDDLE' | 'PRODUCT_SIDEBAR';
export type ConfigType = 'text' | 'color' | 'image' | 'boolean_type' | 'json' | 'number';
export type Gender = 0 | 1 | 2;
