package com.vn.sodu.product.repo;

import com.vn.sodu.product.ProductAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductAttributeRepo extends JpaRepository<ProductAttribute,Long> {

    void deleteByProductId(Long productId);

    List<ProductAttribute> findByProductId(Long productId);
}
