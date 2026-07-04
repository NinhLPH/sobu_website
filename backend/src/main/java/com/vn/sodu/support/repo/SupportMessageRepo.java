package com.vn.sodu.support.repo;

import com.vn.sodu.support.SupportMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupportMessageRepo extends JpaRepository<SupportMessage, Long> {

    Page<SupportMessage> findByConversationIdOrderByCreatedAtDesc(Long conversationId, Pageable pageable);
}
