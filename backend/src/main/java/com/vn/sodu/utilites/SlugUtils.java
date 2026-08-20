package com.vn.sodu.utilites;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SlugUtils {

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");
    private static final Pattern MULTI_HYPHEN = Pattern.compile("-+");

    private SlugUtils() {
    }

    /**
     * Converts a Vietnamese or English text into an SEO-friendly kebab-case slug.
     * Example: "Mô hình Gundam RX-78-2 RG 1/144" -> "mo-hinh-gundam-rx-78-2-rg-1-144"
     */
    public static String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        String text = input.trim();

        // 1. Replace specific Vietnamese letters that standard Normalizer misses (e.g. đ, Đ)
        text = text.replace("đ", "d")
                   .replace("Đ", "d")
                   .replace("ð", "d");

        // 2. Normalize and strip diacritical marks (accents)
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String slug = pattern.matcher(normalized).replaceAll("");

        // 3. Replace non-alphanumeric chars with hyphen
        slug = WHITESPACE.matcher(slug).replaceAll("-");
        slug = slug.toLowerCase(Locale.ENGLISH);
        slug = NONLATIN.matcher(slug).replaceAll("-");

        // 4. Remove leading, trailing, and duplicate hyphens
        slug = MULTI_HYPHEN.matcher(slug).replaceAll("-");
        slug = slug.replaceAll("^-|-$", "");

        return slug;
    }
}
