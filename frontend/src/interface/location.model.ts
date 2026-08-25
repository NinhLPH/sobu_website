export interface LocationProvince {
    id: number;
    name: string;
}

export interface LocationWard {
    id: number;
    name: string;
}

export type LocationSource = 'external-v2' | 'backend-address' | 'backend-fallback';

export interface ExternalLocationProvince {
    code: number;
    name: string;
    codename?: string | null;
    division_type?: string | null;
}

export interface ExternalLocationWard {
    code: number;
    name: string;
    province_code: number;
    codename?: string | null;
    division_type?: string | null;
}

export interface ProvinceListResponse {
    datasetVersion: string;
    provinces: LocationProvince[];
}

export interface WardListResponse {
    datasetVersion: string;
    wards: LocationWard[];
}

export interface AddressDatasetResponse {
    version: string;
    source?: string | null;
    importedAt?: string | null;
    checksum?: string | null;
    provinceCount: number;
    wardCount: number;
}
