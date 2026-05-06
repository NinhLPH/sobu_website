package com.vn.sodu.global.controller;

import com.vn.sodu.global.dto.PageResponse;
import com.vn.sodu.product.dto.ProductDetailDTO;
import com.vn.sodu.product.dto.ProductFilterRequest;
import com.vn.sodu.product.category.dto.CategoryListItemDTO;
import com.vn.sodu.product.category.service.CategoryService;
import com.vn.sodu.product.dto.ProductListItemDTO;
import com.vn.sodu.product.service.ProductService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springdoc.core.annotations.ParameterObject;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping({"/api/public", "/api/v1/public"})
@Tag(name = "Public Catalog", description = "Guest-facing product catalogue endpoints")
public class PublicController {
    private final ProductService productService;
    private final CategoryService categoryService;

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
            @ParameterObject ProductFilterRequest request
    ) {
        Page<ProductListItemDTO> result = productService.getPublicProducts(request);
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

    @GetMapping("/products/{id}")
    @Operation(
            summary = "Get public product detail",
            description = "Returns the full product detail view for a single product."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ProductDetailDTO.class))),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ProductDetailDTO> getProductDetail(@PathVariable long id) {
        return ResponseEntity.ok(productService.getProductDetailById(id));
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
        Page<ProductListItemDTO> result = productService.searchProducts(query, request);
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
}
