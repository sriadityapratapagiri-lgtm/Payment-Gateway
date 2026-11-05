package com.payflow.gateway.ledger.repository;

import com.payflow.gateway.ledger.entity.LedgerLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LedgerLineRepository extends JpaRepository<LedgerLine, UUID> {
    List<LedgerLine> findByJournalEntryId(UUID journalEntryId);
}

