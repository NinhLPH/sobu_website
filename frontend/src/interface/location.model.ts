export interface LocationProvince {
    id: number;
    name: string;
}

export interface LocationWard {
    id: number;
    name: string;
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
