package com.vn.sodu.seo;

import com.vn.sodu.product.Product;
import com.vn.sodu.product.category.Category;
import com.vn.sodu.product.repo.ProductRepo;
import com.vn.sodu.product.category.CategoryRepo;
import com.vn.sodu.ui.StaticPage;
import com.vn.sodu.ui.StaticPageRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SitemapService {

    private final ProductRepo productRepo;
    private final CategoryRepo categoryRepo;
    private final StaticPageRepo staticPageRepo;

    public List<SitemapUrl> buildSitemap(String baseUrl) {
        List<SitemapUrl> urls = new ArrayList<>();

        String site = baseUrl.replaceAll("/$", "");

        urls.add(new SitemapUrl(site + "/", LocalDate.now(), "daily", "1.0"));
        urls.add(new SitemapUrl(site + "/products", LocalDate.now(), "daily", "0.9"));
        urls.add(new SitemapUrl(site + "/blog", LocalDate.now(), "weekly", "0.8"));
        urls.add(new SitemapUrl(site + "/services", LocalDate.now(), "weekly", "0.7"));
        urls.add(new SitemapUrl(site + "/membership", LocalDate.now(), "weekly", "0.6"));
        urls.add(new SitemapUrl(site + "/about", LocalDate.now(), "monthly", "0.5"));
        urls.add(new SitemapUrl(site + "/privacy", LocalDate.now(), "monthly", "0.3"));
        urls.add(new SitemapUrl(site + "/terms", LocalDate.now(), "monthly", "0.3"));

        List<Product> products = productRepo.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getActive()))
                .toList();
        for (Product p : products) {
            LocalDate lastmod = p.getUpdatedAt() != null
                    ? p.getUpdatedAt().toLocalDate()
                    : LocalDate.now();
            urls.add(new SitemapUrl(site + "/product/" + p.getId(), lastmod, "daily", "0.8"));
        }

        List<Category> categories = categoryRepo.findAll();
        for (Category c : categories) {
            String slug = c.getName() != null
                    ? c.getName().toLowerCase().replaceAll("\\s+", "-").replaceAll("[^a-z0-9-]", "")
                    : "category-" + c.getId();
            urls.add(new SitemapUrl(site + "/category/" + slug, LocalDate.now(), "weekly", "0.7"));
        }

        List<StaticPage> pages = staticPageRepo.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsPublished()))
                .toList();
        for (StaticPage p : pages) {
            LocalDate lastmod = p.getUpdatedAt() != null
                    ? p.getUpdatedAt().toLocalDate()
                    : LocalDate.now();
            urls.add(new SitemapUrl(site + "/" + p.getSlug(), lastmod, "weekly", "0.6"));
        }

        urls.sort(Comparator.comparingDouble(
                u -> Double.parseDouble(u.priority()) * -1));
        return urls;
    }
}
