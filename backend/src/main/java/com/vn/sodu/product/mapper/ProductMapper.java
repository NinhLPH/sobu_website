package com.vn.sodu.product.mapper;

import com.vn.sodu.product.Product;
import com.vn.sodu.product.ProductAttribute;
import com.vn.sodu.product.ProductImage;
import com.vn.sodu.product.ProductUnit;
import com.vn.sodu.product.dto.NhanhProductDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import com.vn.sodu.product.dto.ProductAttributeDTO;
import com.vn.sodu.product.dto.ProductDetailDTO;
import com.vn.sodu.product.dto.ProductListItemDTO;
import com.vn.sodu.product.dto.ProductUnitDTO;
import org.springframework.stereotype.Component;

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
        product.setParentId(dto.getParentId());
        product.setCode(dto.getCode());
        product.setBarcode(dto.getBarcode());
        product.setName(dto.getName());
        product.setOtherName(dto.getOtherName());
        product.setStatus(dto.getStatus());
        product.setVat(dto.getVat());
        product.setDescription(dto.getDescription());
        product.setContent(dto.getContent());
        product.setCountryName(dto.getCountryName());
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

        for (String url : dto.getImages().getOthers()) {
            if (url == null || url.isBlank()) {
                continue;
            }

            ProductImage image = new ProductImage();
            image.setProductId(productId);
            image.setUrl(url);
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

        return ProductListItemDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .code(entity.getCode())
                .price(entity.getRetailPrice())
                .avatarImage(entity.getAvatarImage())
                .brandName(entity.getBrandName())
                .categoryName(entity.getCategoryName())
                .stockAvailable(entity.getStockAvailable())
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

        List<String> imageUrls = null;
        if (images != null) {
            imageUrls = new ArrayList<>(images.size());
            for (ProductImage image : images) {
                if (image == null) {
                    continue;
                }
                if (image.getUrl() == null) {
                    continue;
                }
                imageUrls.add(image.getUrl());
            }
        }

        return ProductDetailDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .code(product.getCode())
                .barcode(product.getBarcode())
                .description(product.getDescription())
                .content(product.getContent())
                .price(product.getRetailPrice())
                .wholesalePrice(product.getWholesalePrice())
                .avatarImage(product.getAvatarImage())
                .brandName(product.getBrandName())
                .categoryName(product.getCategoryName())
                .stockAvailable(product.getStockAvailable())
                .units(unitDTOs)
                .attributes(attributeDTOs)
                .images(imageUrls)
                .build();
    }
}

