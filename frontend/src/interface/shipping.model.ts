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
    carrierId?: number | null;
    carrierName?: string | null;
    carrierServiceId?: number | null;
    carrierServiceName?: string | null;
    shipFee?: number | null;
    customerShipFee?: number | null;
    deliveryTime?: string | null;
    description?: string | null;
}

export interface CarrierConfigDto {
    carrierId?: number | null;
    standardService?: string | null;
    expressService?: string | null;
    expressFallbackId?: number | null;
}

export type CarrierConfigRequestDto = CarrierConfigDto;

export type AdminShippingCarriersPayload = unknown;

export interface NormalizedCarrierItem {
    id?: string;
    name?: string;
    code?: string;
    serviceId?: string;
    serviceName?: string;
    serviceCode?: string;
    raw: unknown;
}
