package com.payflow.gateway.ledger.repository;

import com.payflow.gateway.ledger.entity.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, UUID> {
    List<JournalEntry> findByReferenceIdOrderByCreatedAtDesc(UUID referenceId);
    List<JournalEntry> findAllByOrderByCreatedAtDesc();
    boolean existsByReferenceId(UUID referenceId);
}

