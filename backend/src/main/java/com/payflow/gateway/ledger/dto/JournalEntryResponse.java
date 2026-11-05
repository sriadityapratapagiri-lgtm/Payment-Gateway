package com.payflow.gateway.ledger.dto;

import com.payflow.gateway.ledger.entity.JournalEntry;
import com.payflow.gateway.ledger.entity.LedgerLine;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record JournalEntryResponse(
    UUID id,
    String referenceType,
    UUID referenceId,
    String description,
    String status,
    OffsetDateTime createdAt,
    List<LedgerLineResponse> lines,
    long totalDebits,
    long totalCredits,
    boolean balanced
) {
    public record LedgerLineResponse(
        UUID id,
        String accountCode,
        String accountType,
        String direction,
        Long amountMinor,
        String currency
    ) {}

    public static JournalEntryResponse from(JournalEntry entry) {
        List<LedgerLineResponse> lines = entry.getLines().stream()
            .map(l -> new LedgerLineResponse(
                l.getId(),
                l.getAccount().getAccountCode(),
                l.getAccount().getAccountType(),
                l.getDirection(),
                l.getAmountMinor(),
                l.getCurrency()
            )).toList();

        long debits = entry.getLines().stream()
            .filter(l -> "DEBIT".equals(l.getDirection()))
            .mapToLong(LedgerLine::getAmountMinor).sum();
        long credits = entry.getLines().stream()
            .filter(l -> "CREDIT".equals(l.getDirection()))
            .mapToLong(LedgerLine::getAmountMinor).sum();

        return new JournalEntryResponse(
            entry.getId(),
            entry.getReferenceType(),
            entry.getReferenceId(),
            entry.getDescription(),
            entry.getStatus(),
            entry.getCreatedAt(),
            lines,
            debits,
            credits,
            debits == credits
        );
    }
}

