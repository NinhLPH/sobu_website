package com.vn.sodu.voucher.service;

import com.vn.sodu.voucher.GeoScope;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class VoucherGeoService {

    // Canonical Hanoi-center wards supplied by the business. Store normalized
    // values so comparison is stable across accents, casing and whitespace.
    private static final Set<String> HANOI_CENTER_WARDS = Set.of(
            "phuong hoan kiem", "phuong cua nam", "phuong ba dinh", "phuong ngoc ha",
            "phuong giang vo", "phuong tay ho", "phuong phu thuong", "phuong long bien",
            "phuong bo de", "phuong viet hung", "phuong phuc loi", "phuong dong da",
            "phuong kim lien", "phuong van mieu qtg", "phuong o cho dua", "phuong lang",
            "phuong cau giay", "phuong nghia do", "phuong yen hoa", "phuong thanh xuan",
            "phuong khuong dinh", "phuong phuong liet", "phuong thuong cat", "phuong tay tuu",
            "phuong dong ngac", "phuong xuan dinh", "phuong phu dien", "phuong tu liem",
            "phuong xuan phuong", "phuong tay mo", "phuong dai mo", "phuong hai ba trung",
            "phuong bach mai", "phuong vinh tuy", "phuong hoang mai", "phuong vinh hung",
            "phuong tuong mai", "phuong dinh cong", "phuong hoang liet", "phuong yen so",
            "phuong linh nam", "phuong hong ha"
    );

    private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    public static String normalizeText(String input) {
        if (input == null) {
            return "";
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String withoutDiacritics = DIACRITICS_PATTERN.matcher(normalized).replaceAll("");
        return withoutDiacritics.replace("đ", "d").replace("Đ", "d")
                .toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("\\s+", " ");
    }

    public boolean isAddressEligible(GeoScope geoScope, String cityName, String wardName, Long cityId) {
        if (geoScope == null || geoScope == GeoScope.ALL) {
            return true;
        }

        if (geoScope == GeoScope.HANOI_CENTER) {
            return isHanoiCenter(cityName, wardName, cityId);
        }

        return true;
    }

    public boolean isHanoiCenter(String cityName, String wardName, Long cityId) {
        if (cityName == null && cityId == null) {
            return false;
        }

        // The local address dataset uses province id 1 for Hanoi. If both the
        // id and name are supplied, both must identify Hanoi.
        boolean hasHanoiCityId = cityId != null && cityId == 1L;
        boolean hasHanoiCityName = cityName != null && normalizeText(cityName).contains("ha noi");
        if (cityId != null && !hasHanoiCityId) {
            return false;
        }
        if (cityName != null && !hasHanoiCityName) {
            return false;
        }

        if (!hasHanoiCityId && !hasHanoiCityName) {
            return false;
        }

        if (wardName == null || wardName.isBlank()) {
            return false;
        }

        return HANOI_CENTER_WARDS.contains(normalizeWardName(wardName));
    }

    private String normalizeWardName(String wardName) {
        return normalizeText(wardName)
                .replaceAll("[^a-z0-9]+", " ")
                // The address dataset spells the configured "Văn Miếu-QTG"
                // ward as "Văn Miếu - Quốc Tử Giám".
                .replace("quoc tu giam", "qtg")
                .trim();
    }
}
