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
export type OrderSyncStatus = 'PENDING' | 'SYNCED' | 'NEED_RECONCILE' | 'FAILED' | 'DEAD';
export type NhanhSyncStage =
    'NONE'
    | 'NORMAL_ORDER_CREATED'
    | 'PREORDER_DEPOSIT_CREATED'
    | 'PREORDER_FINAL_UPDATED';
export type PaymentStatus = 'PENDING' | 'PAID' | 'FAILED' | 'CANCELLED' | 'EXPIRED' | 'REFUNDED';
export type PaymentMethod = 'ONLINE' | 'COD';
export type AccountStatus = 'ACTIVE' | 'INACTIVE' | 'BANNED';
export type DeviceType = 'WEB' | 'MOBILE' | 'ALL';
export type BannerPosition =
    'home_hero_carousel'
    | 'site_left_sidebar_banner'
    | 'site_right_sidebar_banner'
    | 'home_section_01_banner'
    | 'home_custom_service_image_primary'
    | 'home_custom_service_image_secondary'
    | 'home_custom_service_image_tertiary'
    | 'home_category_card_01'
    | 'home_category_card_02'
    | 'home_category_card_03'
    | 'home_category_card_04'
    | 'home_category_card_05'
    | 'home_category_card_06'
    | 'home_section_02_banner'
    | 'home_promo_grid_top_left'
    | 'home_promo_grid_bottom_left'
    | 'home_promo_grid_top_right'
    | 'home_promo_grid_bottom_right'
    | 'home_section_03_banner'
    | 'home_section_04_banner';
export type ConfigType = 'text' | 'color' | 'image' | 'boolean_type' | 'json' | 'number';
export type Gender = 0 | 1 | 2;
