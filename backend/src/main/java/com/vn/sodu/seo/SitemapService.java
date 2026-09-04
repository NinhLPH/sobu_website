package com.vn.sodu.seo;

import com.vn.sodu.blog.Article;
import com.vn.sodu.blog.ArticleRepo;
import com.vn.sodu.product.Product;
import com.vn.sodu.product.brand.Brand;
import com.vn.sodu.product.brand.BrandRepo;
import com.vn.sodu.product.category.Category;
import com.vn.sodu.product.category.CategoryRepo;
import com.vn.sodu.product.repo.ProductRepo;
import com.vn.sodu.ui.StaticPage;
import com.vn.sodu.ui.StaticPageRepo;
import com.vn.sodu.utilites.SlugUtils;
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
    private final BrandRepo brandRepo;
    private final ArticleRepo articleRepo;
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

        // 1. Products by Slug
        List<Product> products = productRepo.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getActive()))
                .toList();
        for (Product p : products) {
            LocalDate lastmod = p.getUpdatedAt() != null
                    ? p.getUpdatedAt().toLocalDate()
                    : LocalDate.now();
            String slug = (p.getSlug() != null && !p.getSlug().isBlank())
                    ? p.getSlug()
                    : SlugUtils.toSlug(p.getName());
            if (slug != null && !slug.isBlank()) {
                urls.add(new SitemapUrl(site + "/product/" + slug, lastmod, "daily", "0.8"));
            }
        }

        // 2. Categories by Slug
        List<Category> categories = categoryRepo.findAll();
        for (Category c : categories) {
            String slug = (c.getSlug() != null && !c.getSlug().isBlank())
                    ? c.getSlug()
                    : SlugUtils.toSlug(c.getName());
            if (slug != null && !slug.isBlank()) {
                urls.add(new SitemapUrl(site + "/category/" + slug, LocalDate.now(), "weekly", "0.7"));
            }
        }

        // 3. Brands by Slug
        List<Brand> brands = brandRepo.findAll();
        for (Brand b : brands) {
            String slug = (b.getSlug() != null && !b.getSlug().isBlank())
                    ? b.getSlug()
                    : SlugUtils.toSlug(b.getName());
            if (slug != null && !slug.isBlank()) {
                urls.add(new SitemapUrl(site + "/brand/" + slug, LocalDate.now(), "weekly", "0.6"));
            }
        }

        // 4. Articles by Slug
        List<Article> articles = articleRepo.findAll().stream()
                .filter(a -> "PUBLISHED".equalsIgnoreCase(a.getStatus()))
                .toList();
        for (Article a : articles) {
            LocalDate lastmod = a.getUpdatedAt() != null
                    ? a.getUpdatedAt().toLocalDate()
                    : (a.getPublishedAt() != null ? a.getPublishedAt().toLocalDate() : LocalDate.now());
            urls.add(new SitemapUrl(site + "/blog/" + a.getSlug(), lastmod, "weekly", "0.7"));
        }

        // 5. Static Pages by Slug
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
