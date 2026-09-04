package com.vn.sodu.product.mapper;

import com.vn.sodu.product.Product;
import com.vn.sodu.product.ProductAttribute;
import com.vn.sodu.product.ProductImage;
import com.vn.sodu.product.ProductUnit;
import com.vn.sodu.product.dto.ProductCreateRequest;
import com.vn.sodu.product.dto.ProductUpdateRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class AdminProductMapper {

    public Product toEntity(ProductCreateRequest request) {
        if (request == null) {
            return null;
        }

        Product product = new Product();
        product.setCode(request.getCode());
        product.setBarcode(request.getBarcode());
        product.setName(request.getName());
        product.setOtherName(request.getOtherName());
        product.setCategoryId(request.getCategoryId());
        product.setBrandId(request.getBrandId());
        product.setBadgeId(request.getBadgeId());
        product.setRetailPrice(request.getRetailPrice());
        product.setImportPrice(request.getImportPrice());
        product.setWholesalePrice(request.getWholesalePrice());
        product.setOldPrice(request.getOldPrice());
        product.setSaleValidFrom(request.getSaleValidFrom());
        product.setSaleValidThrough(request.getSaleValidThrough());
        product.setVat(request.getVat());
        product.setAvatarImage(request.getAvatarImage());
        product.setDescription(request.getDescription());
        product.setContent(request.getContent());
        product.setLength(request.getLength());
        product.setWidth(request.getWidth());
        product.setHeight(request.getHeight());
        product.setWeight(request.getWeight());
        product.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");
        product.setActive(request.getActive() != null ? request.getActive() : true);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        return product;
    }

    public void updateEntity(Product product, ProductUpdateRequest request) {
        if (product == null || request == null) {
            return;
        }

        if (request.getCode() != null) product.setCode(request.getCode());
        if (request.getBarcode() != null) product.setBarcode(request.getBarcode());
        if (request.getName() != null) product.setName(request.getName());
        if (request.getOtherName() != null) product.setOtherName(request.getOtherName());
        if (request.getCategoryId() != null) product.setCategoryId(request.getCategoryId());
        if (request.getBrandId() != null) product.setBrandId(request.getBrandId());
        if (request.getBadgeId() != null) product.setBadgeId(request.getBadgeId());
        if (request.getRetailPrice() != null) product.setRetailPrice(request.getRetailPrice());
        if (request.getImportPrice() != null) product.setImportPrice(request.getImportPrice());
        if (request.getWholesalePrice() != null) product.setWholesalePrice(request.getWholesalePrice());
        product.setOldPrice(request.getOldPrice());
        product.setSaleValidFrom(request.getSaleValidFrom());
        product.setSaleValidThrough(request.getSaleValidThrough());
        if (request.getVat() != null) product.setVat(request.getVat());
        if (request.getAvatarImage() != null) product.setAvatarImage(request.getAvatarImage());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getContent() != null) product.setContent(request.getContent());
        if (request.getLength() != null) product.setLength(request.getLength());
        if (request.getWidth() != null) product.setWidth(request.getWidth());
        if (request.getHeight() != null) product.setHeight(request.getHeight());
        if (request.getWeight() != null) product.setWeight(request.getWeight());
        if (request.getStatus() != null) product.setStatus(request.getStatus());
        if (request.getActive() != null) product.setActive(request.getActive());
        product.setUpdatedAt(LocalDateTime.now());
    }

    public List<ProductUnit> toUnits(Long productId, ProductCreateRequest request) {
        List<ProductUnit> result = new ArrayList<>();
        if (productId == null || request == null || request.getUnits() == null) {
            return result;
        }

        for (ProductCreateRequest.ProductUnitRequest item : request.getUnits()) {
            if (item == null) {
                continue;
            }

            ProductUnit unit = new ProductUnit();
            unit.setProductId(productId);
            unit.setName(item.getName());
            unit.setQuantity(item.getQuantity());
            if (item.getPrice() != null) {
                unit.setPrice(item.getPrice());
                unit.setWholesalePrice(item.getWholesalePrice());
            }
            result.add(unit);
        }

        return result;
    }

    public List<ProductUnit> toUnits(Long productId, ProductUpdateRequest request) {
        List<ProductUnit> result = new ArrayList<>();
        if (productId == null || request == null || request.getUnits() == null) {
            return result;
        }

        for (ProductUpdateRequest.ProductUnitRequest item : request.getUnits()) {
            if (item == null) {
                continue;
            }

            ProductUnit unit = new ProductUnit();
            unit.setProductId(productId);
            unit.setName(item.getName());
            unit.setQuantity(item.getQuantity());
            if (item.getPrice() != null) {
                unit.setPrice(item.getPrice());
                unit.setWholesalePrice(item.getWholesalePrice());
            }
            result.add(unit);
        }

        return result;
    }

    public List<ProductAttribute> toAttributes(Long productId, ProductCreateRequest request) {
        List<ProductAttribute> result = new ArrayList<>();
        if (productId == null || request == null || request.getAttributes() == null) {
            return result;
        }

        for (ProductCreateRequest.ProductAttributeRequest attribute : request.getAttributes()) {
            if (attribute == null) {
                continue;
            }

            ProductAttribute entity = new ProductAttribute();
            entity.setProductId(productId);
            entity.setName(attribute.getName());
            entity.setValue(attribute.getValue());
            result.add(entity);
        }

        return result;
    }

    public List<ProductAttribute> toAttributes(Long productId, ProductUpdateRequest request) {
        List<ProductAttribute> result = new ArrayList<>();
        if (productId == null || request == null || request.getAttributes() == null) {
            return result;
        }

        for (ProductUpdateRequest.ProductAttributeRequest attribute : request.getAttributes()) {
            if (attribute == null) {
                continue;
            }

            ProductAttribute entity = new ProductAttribute();
            entity.setProductId(productId);
            entity.setName(attribute.getName());
            entity.setValue(attribute.getValue());
            result.add(entity);
        }

        return result;
    }

    public List<ProductImage> toImages(Long productId, ProductCreateRequest request) {
        List<ProductImage> result = new ArrayList<>();
        if (productId == null || request == null || request.getImages() == null) {
            return result;
        }

        for (String url : request.getImages()) {
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

    public List<ProductImage> toImages(Long productId, ProductUpdateRequest request) {
        List<ProductImage> result = new ArrayList<>();
        if (productId == null || request == null || request.getImages() == null) {
            return result;
        }

        for (String url : request.getImages()) {
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
}
