package com.vn.sodu.product.repo;

import com.vn.sodu.product.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ProductImageRepo extends JpaRepository<ProductImage,Long> {

    @Transactional
    void deleteByProductId(Long productId);

    List<ProductImage> findByProductId(Long productId);
}
