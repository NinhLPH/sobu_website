export interface ShippingQuoteRequestDto {
    customerAddress?: string;
    customerCityId: number;
    customerDistrictId: number;
    customerWardId: number;
    cartSubtotal: number;
    codAmount?: number;
    shippingWeight?: number;
    carrierId?: number;
    carrierServiceId?: number;
}

export interface ShippingQuoteDto {
    carrierId: number;
    carrierName: string;
    carrierServiceId: number;
    carrierServiceName: string;
    shipFee: number;
    customerShipFee?: number | null;
    deliveryTime?: string | null;
    description?: string | null;
}
