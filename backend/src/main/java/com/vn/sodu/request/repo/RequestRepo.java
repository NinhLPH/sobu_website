package com.vn.sodu.request.repo;

import com.vn.sodu.request.Request;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface RequestRepo extends JpaRepository<Request, Long> {
    Optional<Request> findByRequestCode(String requestCode);
    Page<Request> findByCustomerPhone(String customerPhone, Pageable pageable);
    Optional<Request> findByIdAndCustomerPhone(Long id, String customerPhone);
}
