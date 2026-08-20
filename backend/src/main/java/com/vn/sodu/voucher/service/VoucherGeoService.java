package com.vn.sodu.voucher.service;

import com.vn.sodu.voucher.GeoScope;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class VoucherGeoService {

    // 11 Central Hanoi Districts normalized (no accents, lowercase, trimmed)
    private static final Set<String> HANOI_CENTER_DISTRICTS = new HashSet<>(Arrays.asList(
            "hoan kiem",
            "ba dinh",
            "dong da",
            "hai ba trung",
            "cau giay",
            "thanh xuan",
            "tay ho",
            "hoang mai",
            "nam tu liem",
            "bac tu liem",
            "ha dong"
    ));

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

    public boolean isAddressEligible(GeoScope geoScope, String cityName, String districtName, Long cityId, Long districtId) {
        if (geoScope == null || geoScope == GeoScope.ALL) {
            return true;
        }

        if (geoScope == GeoScope.HANOI_CENTER) {
            return isHanoiCenter(cityName, districtName, cityId, districtId);
        }

        return true;
    }

    public boolean isHanoiCenter(String cityName, String districtName, Long cityId, Long districtId) {
        if (cityName == null && cityId == null) {
            return false;
        }

        // Check City
        boolean isHanoiCity = false;
        if (cityId != null && cityId == 1L) {
            isHanoiCity = true;
        } else if (cityName != null) {
            String normCity = normalizeText(cityName);
            if (normCity.contains("ha noi")) {
                isHanoiCity = true;
            }
        }

        if (!isHanoiCity) {
            return false;
        }

        // Check District
        if (districtName == null) {
            return false;
        }

        String normDistrict = normalizeText(districtName)
                .replace("quan ", "")
                .replace("huyen ", "")
                .replace("thi xa ", "")
                .trim();

        return HANOI_CENTER_DISTRICTS.contains(normDistrict);
    }
}
