package com.vn.sodu.product.repo;

import com.vn.sodu.product.ProductUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductUnitRepo extends JpaRepository<ProductUnit, Long> {

    void deleteByProductId(Long productId);

    List<ProductUnit> findByProductId(Long productId);
}
