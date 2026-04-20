package com.vn.sodu.product.repo;

import com.vn.sodu.product.ProductUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductUnitRepo extends JpaRepository<ProductUnit, Long> {
}
