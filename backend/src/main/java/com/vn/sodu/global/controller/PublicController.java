package com.vn.sodu.global.controller;

import com.vn.sodu.global.dto.PageResponse;
import com.vn.sodu.product.brand.dto.BrandDTO;
import com.vn.sodu.product.brand.dto.BrandListItemDTO;
import com.vn.sodu.product.brand.service.BrandService;
import com.vn.sodu.product.category.dto.CategoryDTO;
import com.vn.sodu.product.category.dto.CategoryListItemDTO;
import com.vn.sodu.product.category.service.CategoryService;
import com.vn.sodu.product.dto.ProductDetailDTO;
import com.vn.sodu.product.dto.ProductFilterRequest;
import com.vn.sodu.product.dto.ProductListItemDTO;
import com.vn.sodu.product.service.ProductService;
import com.vn.sodu.seo.SlugHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springdoc.core.annotations.ParameterObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping({"/api/public", "/api/v1/public"})
@Tag(name = "Public Catalog", description = "Guest-facing product catalogue endpoints")
public class PublicController {
    private final ProductService productService;
    private final BrandService brandService;
    private final CategoryService categoryService;
    private final SlugHistoryService slugHistoryService;

    @GetMapping("/products")
    @Operation(
            summary = "Get public product catalogue",
            description = "Returns a paginated, filterable list of products for guest users."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Products retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PageResponse.class)))
    })
    public ResponseEntity<PageResponse<ProductListItemDTO>> getAllProducts(
            @Parameter(description = "Search text", example = "shirt")
            @RequestParam(name = "q", required = false) String query,
            @ParameterObject ProductFilterRequest request
    ) {
        Page<ProductListItemDTO> result = productService.getPublicProducts(applySearchFallback(request, query));
        return ResponseEntity.ok(PageResponse.from(result));
    }

    @GetMapping("/products/all")
    @Operation(
            summary = "Get all public products",
            description = "Returns the full public product list without pagination."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Products retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = ProductListItemDTO.class))))
    })
    public ResponseEntity<List<ProductListItemDTO>> getAllProductList() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/products/{slugOrId}")
    @Operation(
            summary = "Get public product detail by slug or ID",
            description = "Returns the full product detail view with SEO metadata for a single product."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ProductDetailDTO.class))),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ProductDetailDTO> getProductDetail(@PathVariable String slugOrId) {
        return ResponseEntity.ok(productService.getProductDetailBySlug(slugOrId));
    }

    @GetMapping("/products/search")
    @Operation(
            summary = "Search public products",
            description = "Searches the public product catalogue by a query string and optional filters."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search completed successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PageResponse.class)))
    })
    public ResponseEntity<PageResponse<ProductListItemDTO>> searchProducts(
            @Parameter(description = "Search text", required = true, example = "shirt")
            @RequestParam("q") String query,
            @ParameterObject ProductFilterRequest request
    ) {
        Page<ProductListItemDTO> result = productService.getPublicProducts(applySearchFallback(request, query));
        return ResponseEntity.ok(PageResponse.from(result));
    }

    @PostMapping("/products/search")
    @Operation(
            summary = "Search public products with request body",
            description = "Searches the public product catalogue with a JSON filter body to avoid long query strings."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search completed successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PageResponse.class)))
    })
    public ResponseEntity<PageResponse<ProductListItemDTO>> searchProducts(
            @RequestBody(required = false) ProductFilterRequest request
    ) {
        Page<ProductListItemDTO> result = productService.getPublicProducts(request);
        return ResponseEntity.ok(PageResponse.from(result));
    }

    @GetMapping("/categories")
    @Operation(
            summary = "Get all public categories",
            description = "Returns the list of all categories for guest users."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Categories retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = CategoryListItemDTO.class))))
    })
    public ResponseEntity<List<CategoryListItemDTO>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAll());
    }

    @GetMapping("/categories/{slugOrId}")
    @Operation(
            summary = "Get public category detail by slug or ID",
            description = "Returns the full category detail view with SEO metadata."
    )
    public ResponseEntity<CategoryDTO> getCategoryDetail(@PathVariable String slugOrId) {
        return ResponseEntity.ok(categoryService.getBySlug(slugOrId));
    }

    @GetMapping("/brands")
    @Operation(
            summary = "Get all public brands",
            description = "Returns the list of all brands for guest users."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Brands retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = BrandListItemDTO.class))))
    })
    public ResponseEntity<List<BrandListItemDTO>> getAllBrands() {
        return ResponseEntity.ok(brandService.getAll());
    }

    @GetMapping("/brands/{slugOrId}")
    @Operation(
            summary = "Get public brand detail by slug or ID",
            description = "Returns the full brand detail view with SEO metadata."
    )
    public ResponseEntity<BrandDTO> getBrandDetail(@PathVariable String slugOrId) {
        return ResponseEntity.ok(brandService.getBySlug(slugOrId));
    }

    @GetMapping("/seo/resolve-url")
    @Operation(
            summary = "Resolve redirect for old slug",
            description = "Checks if an old slug exists and returns the current canonical slug with 301 status."
    )
    public ResponseEntity<Map<String, Object>> resolveRedirect(
            @RequestParam String type,
            @RequestParam String slug
    ) {
        var currentSlugOpt = slugHistoryService.findCurrentSlug(type, slug);
        Map<String, Object> response = new HashMap<>();
        if (currentSlugOpt.isPresent()) {
            response.put("redirect", true);
            response.put("status", 301);
            response.put("currentSlug", currentSlugOpt.get());
        } else {
            response.put("redirect", false);
            response.put("currentSlug", slug);
        }
        return ResponseEntity.ok(response);
    }

    private ProductFilterRequest applySearchFallback(ProductFilterRequest request, String query) {
        ProductFilterRequest safeRequest = request == null ? new ProductFilterRequest() : request;
        if ((safeRequest.getSearch() == null || safeRequest.getSearch().isBlank())
                && query != null
                && !query.isBlank()) {
            safeRequest.setSearch(query);
        }
        return safeRequest;
    }
}
