package com.vn.sodu.product.mapper;

import com.vn.sodu.product.Product;
import com.vn.sodu.product.ProductAttribute;
import com.vn.sodu.product.ProductImage;
import com.vn.sodu.product.ProductUnit;
import com.vn.sodu.product.dto.*;
import com.vn.sodu.seo.dto.SeoMetadataDTO;
import com.vn.sodu.utilites.SlugUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Component
public class ProductMapper {

    public Product toEntity(NhanhProductDTO dto) {
        if (dto == null) {
            return null;
        }

        Product product = new Product();
        product.setId(dto.getId());
        product.setExternalId(dto.getId());
        product.setParentId(dto.getParentId());
        product.setCode(dto.getCode());
        product.setBarcode(dto.getBarcode());
        product.setName(dto.getName());
        product.setSlug(SlugUtils.toSlug(dto.getName()));
        product.setOtherName(dto.getOtherName());
        product.setStatus(dto.getStatus());
        product.setVat(dto.getVat());
        product.setDescription(dto.getDescription());
        product.setContent(dto.getContent());
        product.setCountryName(dto.getCountryName());
        product.setCurrency("VND");
        product.setConditionType("NEW");
        product.setRawData(null);

        if (dto.getCategory() != null) {
            product.setCategoryId(dto.getCategory().getId());
            product.setCategoryName(dto.getCategory().getName());
        }
        if (dto.getInternalCategory() != null) {
            product.setInternalCategoryId(dto.getInternalCategory().getId());
            product.setInternalCategoryName(dto.getInternalCategory().getName());
        }
        if (dto.getBrand() != null) {
            product.setBrandId(dto.getBrand().getId());
            product.setBrandName(dto.getBrand().getName());
        }
        if (dto.getType() != null) {
            product.setTypeId(dto.getType().getId());
            product.setTypeName(dto.getType().getName());
        }
        if (dto.getSuppliers() != null) {
            product.setSupplierId(dto.getSuppliers().getId());
            product.setSupplierName(dto.getSuppliers().getName());
            product.setSupplierPhone(dto.getSuppliers().getMobile());
        }
        if (dto.getPrices() != null) {
            product.setRetailPrice(toBigDecimal(dto.getPrices().getRetail()));
            product.setWholesalePrice(toBigDecimal(dto.getPrices().getWholesale()));
            product.setImportPrice(toBigDecimal(dto.getPrices().getImportPrice()));
            product.setOldPrice(toBigDecimal(dto.getPrices().getOld()));
            product.setAvgCost(toBigDecimal(dto.getPrices().getAvgCost()));
        }
        if (dto.getImages() != null) {
            product.setAvatarImage(dto.getImages().getAvatar());
        }
        if (dto.getShipping() != null) {
            product.setLength(dto.getShipping().getLength());
            product.setWidth(dto.getShipping().getWidth());
            product.setHeight(dto.getShipping().getHeight());
            product.setWeight(dto.getShipping().getWeight());
        }
        if (dto.getInventory() != null) {
            product.setStockRemain(dto.getInventory().getRemain());
            product.setStockAvailable(dto.getInventory().getAvailable());
            product.setAvailability((dto.getInventory().getAvailable() != null && dto.getInventory().getAvailable() > 0)
                    ? "IN_STOCK" : "OUT_OF_STOCK");
        }

        product.setCreatedAt(toLocalDateTime(dto.getCreatedAt()));
        product.setUpdatedAt(toLocalDateTime(dto.getUpdatedAt()));
        return product;
    }

    public List<ProductUnit> toUnits(Long productId, NhanhProductDTO dto) {
        List<ProductUnit> result = new ArrayList<>();
        if (productId == null || dto == null || dto.getUnits() == null || dto.getUnits().getList() == null) {
            return result;
        }

        for (NhanhProductDTO.UnitItem item : dto.getUnits().getList()) {
            if (item == null) {
                continue;
            }

            ProductUnit unit = new ProductUnit();
            unit.setId(item.getId());
            unit.setProductId(productId);
            unit.setName(item.getName());
            unit.setQuantity(toInteger(item.getQuantity()));
            if (item.getPrice() != null) {
                unit.setPrice(toBigDecimal(item.getPrice().getRetail()));
                unit.setWholesalePrice(toBigDecimal(item.getPrice().getWholesale()));
            }
            result.add(unit);
        }

        return result;
    }

    public List<ProductAttribute> toAttributes(Long productId, NhanhProductDTO dto) {
        List<ProductAttribute> result = new ArrayList<>();
        if (productId == null || dto == null || dto.getAttributes() == null) {
            return result;
        }

        for (NhanhProductDTO.Attribute attribute : dto.getAttributes()) {
            if (attribute == null) {
                continue;
            }

            ProductAttribute entity = new ProductAttribute();
            entity.setId(attribute.getId());
            entity.setProductId(productId);
            entity.setName(attribute.getName());
            entity.setValue(attribute.getValue());
            result.add(entity);
        }

        return result;
    }

    public List<ProductImage> toImages(Long productId, NhanhProductDTO dto) {
        List<ProductImage> result = new ArrayList<>();
        if (productId == null || dto == null || dto.getImages() == null || dto.getImages().getOthers() == null) {
            return result;
        }

        int sortOrder = 0;
        for (String url : dto.getImages().getOthers()) {
            if (url == null || url.isBlank()) {
                continue;
            }

            ProductImage image = new ProductImage();
            image.setProductId(productId);
            image.setUrl(url);
            image.setAltText(dto.getName());
            image.setSortOrder(++sortOrder);
            result.add(image);
        }

        return result;
    }

    private BigDecimal toBigDecimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }

    private Integer toInteger(Double value) {
        return value == null ? null : (int) Math.round(value);
    }

    private LocalDateTime toLocalDateTime(Long timestamp) {
        if (timestamp == null) {
            return null;
        }

        long normalized = timestamp;
        if (Math.abs(normalized) < 1_000_000_000_000L) {
            return LocalDateTime.ofInstant(Instant.ofEpochSecond(normalized), ZoneId.systemDefault());
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(normalized), ZoneId.systemDefault());
    }

    public ProductListItemDTO toListItem(Product entity) {
        if (entity == null) {
            return null;
        }

        String brandSlug = entity.getBrand() != null && entity.getBrand().getSlug() != null
                ? entity.getBrand().getSlug()
                : (entity.getBrandName() != null ? SlugUtils.toSlug(entity.getBrandName()) : null);

        String categorySlug = entity.getCategory() != null && entity.getCategory().getSlug() != null
                ? entity.getCategory().getSlug()
                : (entity.getCategoryName() != null ? SlugUtils.toSlug(entity.getCategoryName()) : null);

        String slug = entity.getSlug() != null && !entity.getSlug().isBlank()
                ? entity.getSlug()
                : SlugUtils.toSlug(entity.getName());

        return ProductListItemDTO.builder()
                .id(entity.getId())
                .externalId(entity.getExternalId())
                .name(entity.getName())
                .slug(slug)
                .code(entity.getCode())
                .sku(entity.getCode())
                .price(entity.getRetailPrice())
                .oldPrice(entity.getOldPrice())
                .salePrice(entity.getSalePrice() != null ? entity.getSalePrice() : entity.getRetailPrice())
                .currency(entity.getCurrency() != null ? entity.getCurrency() : "VND")
                .status(entity.getStatus())
                .conditionType(entity.getConditionType() != null ? entity.getConditionType() : "NEW")
                .availability(entity.getAvailability() != null ? entity.getAvailability() : "IN_STOCK")
                .avatarImage(entity.getAvatarImage())
                .avatarAltText(entity.getName())
                .brandId(entity.getBrandId())
                .brandName(entity.getBrandName())
                .brandSlug(brandSlug)
                .categoryId(entity.getCategoryId())
                .categoryName(entity.getCategoryName())
                .categorySlug(categorySlug)
                .stockAvailable(entity.getStockAvailable())
                .active(entity.getActive())
                .badgeId(entity.getBadgeId())
                .badgeName(entity.getBadgeName())
                .badgeColor(entity.getBadgeColor())
                .badgeTextColor(entity.getBadgeTextColor())
                .build();
    }

    public ProductDetailDTO toDetail(
            Product product,
            List<ProductUnit> units,
            List<ProductAttribute> attributes,
            List<ProductImage> images
    ) {
        if (product == null) {
            return null;
        }

        List<ProductUnitDTO> unitDTOs = null;
        if (units != null) {
            unitDTOs = new ArrayList<>(units.size());
            for (ProductUnit unit : units) {
                if (unit == null) {
                    continue;
                }
                ProductUnitDTO dto = new ProductUnitDTO();
                dto.setId(unit.getId());
                dto.setName(unit.getName());
                dto.setQuantity(unit.getQuantity());
                dto.setPrice(unit.getPrice());
                unitDTOs.add(dto);
            }
        }

        List<ProductAttributeDTO> attributeDTOs = null;
        if (attributes != null) {
            attributeDTOs = new ArrayList<>(attributes.size());
            for (ProductAttribute attribute : attributes) {
                if (attribute == null) {
                    continue;
                }
                ProductAttributeDTO dto = new ProductAttributeDTO();
                dto.setName(attribute.getName());
                dto.setValue(attribute.getValue());
                attributeDTOs.add(dto);
            }
        }

        List<String> imageUrls = new ArrayList<>();
        List<ProductImageDTO> imageDetails = new ArrayList<>();
        if (images != null) {
            for (ProductImage image : images) {
                if (image == null || image.getUrl() == null) {
                    continue;
                }
                imageUrls.add(image.getUrl());
                imageDetails.add(ProductImageDTO.builder()
                        .id(image.getId())
                        .url(image.getUrl())
                        .altText(image.getAltText() != null ? image.getAltText() : product.getName())
                        .caption(image.getCaption())
                        .width(image.getWidth())
                        .height(image.getHeight())
                        .sortOrder(image.getSortOrder())
                        .isAvatar(image.getIsAvatar())
                        .build());
            }
        }

        String brandSlug = product.getBrand() != null && product.getBrand().getSlug() != null
                ? product.getBrand().getSlug()
                : (product.getBrandName() != null ? SlugUtils.toSlug(product.getBrandName()) : null);

        String categorySlug = product.getCategory() != null && product.getCategory().getSlug() != null
                ? product.getCategory().getSlug()
                : (product.getCategoryName() != null ? SlugUtils.toSlug(product.getCategoryName()) : null);

        String slug = product.getSlug() != null && !product.getSlug().isBlank()
                ? product.getSlug()
                : SlugUtils.toSlug(product.getName());

        String seoTitle = product.getSeoTitle() != null && !product.getSeoTitle().isBlank()
                ? product.getSeoTitle()
                : product.getName() + " | Sobu";

        String metaDesc = product.getMetaDescription() != null && !product.getMetaDescription().isBlank()
                ? product.getMetaDescription()
                : (product.getDescription() != null ? product.getDescription() : product.getName());

        SeoMetadataDTO seo = SeoMetadataDTO.builder()
                .seoTitle(seoTitle)
                .metaDescription(metaDesc)
                .canonicalUrl(product.getCanonicalUrl())
                .robots("index, follow")
                .ogTitle(seoTitle)
                .ogDescription(metaDesc)
                .ogImage(product.getAvatarImage())
                .ogType("product")
                .build();

        return ProductDetailDTO.builder()
                .id(product.getId())
                .externalId(product.getExternalId())
                .name(product.getName())
                .slug(slug)
                .h1Title(product.getH1Title() != null ? product.getH1Title() : product.getName())
                .otherName(product.getOtherName())
                .code(product.getCode())
                .sku(product.getCode())
                .barcode(product.getBarcode())
                .status(product.getStatus())
                .description(product.getDescription())
                .content(product.getContent())
                .price(product.getRetailPrice())
                .wholesalePrice(product.getWholesalePrice())
                .oldPrice(product.getOldPrice())
                .salePrice(product.getSalePrice() != null ? product.getSalePrice() : product.getRetailPrice())
                .saleValidFrom(product.getSaleValidFrom())
                .saleValidThrough(product.getSaleValidThrough())
                .currency(product.getCurrency() != null ? product.getCurrency() : "VND")
                .vat(product.getVat())
                .conditionType(product.getConditionType() != null ? product.getConditionType() : "NEW")
                .availability(product.getAvailability() != null ? product.getAvailability() : "IN_STOCK")
                .preorderExpectedDate(product.getPreorderExpectedDate())
                .avatarImage(product.getAvatarImage())
                .avatarAltText(product.getName())
                .brandId(product.getBrandId())
                .brandName(product.getBrandName())
                .brandSlug(brandSlug)
                .categoryId(product.getCategoryId())
                .categoryName(product.getCategoryName())
                .categorySlug(categorySlug)
                .stockAvailable(product.getStockAvailable())
                .stockRemain(product.getStockRemain())
                .units(unitDTOs)
                .attributes(attributeDTOs)
                .images(imageUrls)
                .imageDetails(imageDetails)
                .updatedAt(product.getUpdatedAt())
                .active(product.getActive())
                .badgeId(product.getBadgeId())
                .badgeName(product.getBadgeName())
                .badgeColor(product.getBadgeColor())
                .badgeTextColor(product.getBadgeTextColor())
                .seo(seo)
                .build();
    }
}
