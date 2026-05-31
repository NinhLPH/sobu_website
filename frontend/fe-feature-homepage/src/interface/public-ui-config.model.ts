import {BannerPosition, ConfigType, DeviceType} from "../enum/union-types";

export interface BannerDTO {
    id: number;
    title: string;
    imageUrl: string;
    linkUrl?: string;
    displayOrder?: number;
    position: BannerPosition;
    isActive: boolean;
    startDate?: string;
    endDate?: string;
    deviceType: DeviceType;
    createdAt?: string;
    updatedAt?: string;
}

export interface WebsiteConfigurationDTO {
    id: number;
    key: string;
    value: string;
    type: ConfigType;
    groupName?: string;
    description?: string;
    isPublic: boolean;
    createdAt?: string;
    updatedAt?: string;
}