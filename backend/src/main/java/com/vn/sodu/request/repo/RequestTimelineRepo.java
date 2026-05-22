package com.vn.sodu.request.repo;

import com.vn.sodu.request.RequestTimeline;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RequestTimelineRepo extends JpaRepository<RequestTimeline, Long> {
    List<RequestTimeline> findByRequestIdOrderByCreatedAtAsc(Long requestId);
}
