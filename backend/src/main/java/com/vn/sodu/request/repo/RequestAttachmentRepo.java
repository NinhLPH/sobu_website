package com.vn.sodu.request.repo;

import com.vn.sodu.request.RequestAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RequestAttachmentRepo extends JpaRepository<RequestAttachment, Long> {
    List<RequestAttachment> findByRequestId(Long requestId);
}
