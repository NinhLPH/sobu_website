package com.vn.sodu.request.repo;

import com.vn.sodu.request.RequestSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RequestSnapshotRepo extends JpaRepository<RequestSnapshot, Long> {
    List<RequestSnapshot> findByRequestIdOrderByCapturedAtDesc(Long requestId);
}
