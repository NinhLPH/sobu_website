export interface LocationWard {
    wardId: number;
    wardName: string;
    otherName: string | null;
}

export interface LocationDistrict {
    districtId: number;
    districtName: string;
    otherName: string | null;
    wards: LocationWard[];
}

export interface LocationCity {
    cityId: number;
    cityName: string;
    otherName: string | null;
    districts: LocationDistrict[];
}

export interface LocationTreeResponse {
    provider: string;
    locationVersion: string;
    cachedAt: string;
    expiresAt: string;
    stale: boolean;
    cities: LocationCity[];
}
