package com.vn.sodu.support.repo;

import com.vn.sodu.support.SupportConversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SupportConversationRepo extends JpaRepository<SupportConversation, Long> {

    @EntityGraph(attributePaths = "account")
    Optional<SupportConversation> findByAccountId(Long accountId);

    @Override
    @EntityGraph(attributePaths = "account")
    Optional<SupportConversation> findById(Long id);

    @EntityGraph(attributePaths = "account")
    Page<SupportConversation> findAllByOrderByLastMessageAtDesc(Pageable pageable);
}
