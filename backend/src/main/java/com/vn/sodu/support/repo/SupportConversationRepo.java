package com.vn.sodu.support.repo;

import com.vn.sodu.support.SupportConversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SupportConversationRepo extends JpaRepository<SupportConversation, Long> {

    Optional<SupportConversation> findByAccountId(Long accountId);

    Page<SupportConversation> findAllByOrderByLastMessageAtDesc(Pageable pageable);
}
