package com.vn.sodu.utilites;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SlugUtilsTest {

    @Test
    void testVietnameseSlugConversion() {
        assertEquals("mo-hinh-gundam-rx-78-2-rg-1-144", SlugUtils.toSlug("Mô hình Gundam RX-78-2 RG 1/144"));
        assertEquals("ao-so-mi-nam-vai-dui-cao-cap", SlugUtils.toSlug("Áo Sơ Mi Nam Vải Đũi Cao Cấp"));
        assertEquals("dien-thoai-iphone-15-pro-max-256gb", SlugUtils.toSlug("Điện thoại iPhone 15 Pro Max 256GB"));
        assertEquals("do-choi-tre-em-100-chinh-hang", SlugUtils.toSlug("Đồ chơi trẻ em 100% chính hãng!"));
        assertEquals("", SlugUtils.toSlug(null));
        assertEquals("", SlugUtils.toSlug("   "));
    }
}
