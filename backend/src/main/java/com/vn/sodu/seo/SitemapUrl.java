package com.vn.sodu.seo;

import java.time.LocalDate;

public record SitemapUrl(
        String loc,
        LocalDate lastmod,
        String changefreq,
        String priority
) {}
