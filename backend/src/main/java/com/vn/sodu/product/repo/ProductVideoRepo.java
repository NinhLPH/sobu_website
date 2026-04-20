package com.vn.sodu.product.repo;

import com.vn.sodu.product.ProductVideo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductVideoRepo extends JpaRepository<ProductVideo,Long> {
}
