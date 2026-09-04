package com.vn.sodu.seo;

import com.vn.sodu.blog.Article;
import com.vn.sodu.blog.ArticleRepo;
import com.vn.sodu.product.Product;
import com.vn.sodu.product.brand.Brand;
import com.vn.sodu.product.brand.BrandRepo;
import com.vn.sodu.product.category.Category;
import com.vn.sodu.product.category.CategoryRepo;
import com.vn.sodu.product.repo.ProductRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ProductSeoApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private CategoryRepo categoryRepo;

    @Autowired
    private BrandRepo brandRepo;

    @Autowired
    private ArticleRepo articleRepo;

    @Autowired
    private SlugHistoryService slugHistoryService;

    @BeforeEach
    void setup() {
        if (categoryRepo.findBySlug("mo-hinh-lap-rap").isEmpty()) {
            Category category = Category.builder()
                    .name("Mô hình lắp ráp")
                    .slug("mo-hinh-lap-rap")
                    .seoTitle("Mô Hình Lắp Ráp Gundam | Sobu")
                    .metaDescription("Bộ sưu tập mô hình lắp ráp chính hãng.")
                    .status(1)
                    .build();
            categoryRepo.save(category);
        }

        if (brandRepo.findBySlug("bandai-spirits").isEmpty()) {
            Brand brand = Brand.builder()
                    .name("Bandai Spirits")
                    .slug("bandai-spirits")
                    .seoTitle("Mô Hình Bandai Chính Hãng | Sobu")
                    .metaDescription("Thương hiệu Bandai Spirits nổi tiếng.")
                    .status(1)
                    .build();
            brandRepo.save(brand);
        }

        if (productRepo.findBySlug("mo-hinh-gundam-rg-rx-78-2").isEmpty()) {
            Product product = Product.builder()
                    .code("GUNDAM-RG-01")
                    .name("Mô hình Gundam RG RX-78-2")
                    .slug("mo-hinh-gundam-rg-rx-78-2")
                    .seoTitle("Mô hình Gundam RG RX-78-2 Chính Hãng | Sobu")
                    .metaDescription("Mua Gundam RG RX-78-2 chính hãng giá tốt tại Sobu.")
                    .retailPrice(BigDecimal.valueOf(650000))
                    .oldPrice(BigDecimal.valueOf(750000))
                    .status("ACTIVE")
                    .active(true)
                    .stockAvailable(10.0)
                    .stockRemain(10.0)
                    .currency("VND")
                    .conditionType("NEW")
                    .availability("IN_STOCK")
                    .build();
            productRepo.save(product);
        }

        if (articleRepo.findBySlug("huong-dan-rap-gundam-cho-nguoi-moi").isEmpty()) {
            Article article = Article.builder()
                    .title("Hướng dẫn ráp Gundam cho người mới")
                    .slug("huong-dan-rap-gundam-cho-nguoi-moi")
                    .seoTitle("Hướng Dẫn Ráp Gundam Cơ Bản 2026 | Sobu")
                    .metaDescription("Chi tiết các bước cắt, gọt và ráp Gundam chuẩn cho beginner.")
                    .content("<p>Nội dung bài viết ráp Gundam...</p>")
                    .status("PUBLISHED")
                    .publishedAt(LocalDateTime.now())
                    .build();
            articleRepo.save(article);
        }
    }

    @Test
    void testGetProductDetailBySlug() throws Exception {
        mockMvc.perform(get("/api/public/products/mo-hinh-gundam-rg-rx-78-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mô hình Gundam RG RX-78-2"))
                .andExpect(jsonPath("$.slug").value("mo-hinh-gundam-rg-rx-78-2"))
                .andExpect(jsonPath("$.sku").value("GUNDAM-RG-01"))
                .andExpect(jsonPath("$.seo.seoTitle").value("Mô hình Gundam RG RX-78-2 Chính Hãng | Sobu"))
                .andExpect(jsonPath("$.seo.robots").value("index, follow"));
    }

    @Test
    void testGetCategoryDetailBySlug() throws Exception {
        mockMvc.perform(get("/api/public/categories/mo-hinh-lap-rap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mô hình lắp ráp"))
                .andExpect(jsonPath("$.slug").value("mo-hinh-lap-rap"))
                .andExpect(jsonPath("$.seo.seoTitle").value("Mô Hình Lắp Ráp Gundam | Sobu"));
    }

    @Test
    void testGetBrandDetailBySlug() throws Exception {
        mockMvc.perform(get("/api/public/brands/bandai-spirits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Bandai Spirits"))
                .andExpect(jsonPath("$.slug").value("bandai-spirits"));
    }

    @Test
    void testGetArticleBySlug() throws Exception {
        mockMvc.perform(get("/api/public/articles/huong-dan-rap-gundam-cho-nguoi-moi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Hướng dẫn ráp Gundam cho người mới"))
                .andExpect(jsonPath("$.slug").value("huong-dan-rap-gundam-cho-nguoi-moi"))
                .andExpect(jsonPath("$.seo.ogType").value("article"));
    }

    @Test
    void testSitemapXmlGeneration() throws Exception {
        mockMvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("mo-hinh-gundam-rg-rx-78-2")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("mo-hinh-lap-rap")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("huong-dan-rap-gundam-cho-nguoi-moi")));
    }

    @Test
    void testRobotsTxt() throws Exception {
        mockMvc.perform(get("/robots.txt"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sitemap:")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Disallow: /admin/")));
    }

    @Test
    void testSlugHistoryRedirectResolution() throws Exception {
        slugHistoryService.recordSlugChange("PRODUCT", 999L, "old-gundam-slug", "new-gundam-slug");

        mockMvc.perform(get("/api/public/seo/resolve-url")
                        .param("type", "PRODUCT")
                        .param("slug", "old-gundam-slug"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.redirect").value(true))
                .andExpect(jsonPath("$.status").value(301))
                .andExpect(jsonPath("$.currentSlug").value("new-gundam-slug"));
    }
}
