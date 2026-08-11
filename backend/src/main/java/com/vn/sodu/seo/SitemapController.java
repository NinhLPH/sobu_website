package com.vn.sodu.seo;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SitemapController {

    private final SitemapService sitemapService;

    @Value("${app.frontend.base-url}")
    private String baseUrl;

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getSitemap() {
        List<SitemapUrl> urls = sitemapService.buildSitemap(baseUrl);

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        for (SitemapUrl url : urls) {
            xml.append("  <url>\n");
            xml.append("    <loc>").append(escapeXml(url.loc())).append("</loc>\n");
            xml.append("    <lastmod>").append(url.lastmod()).append("</lastmod>\n");
            xml.append("    <changefreq>").append(url.changefreq()).append("</changefreq>\n");
            xml.append("    <priority>").append(url.priority()).append("</priority>\n");
            xml.append("  </url>\n");
        }

        xml.append("</urlset>\n");
        return ResponseEntity.ok(xml.toString());
    }

    private String escapeXml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
