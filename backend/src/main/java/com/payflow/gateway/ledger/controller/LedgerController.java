package com.payflow.gateway.ledger.controller;

import com.payflow.gateway.common.dto.ApiResponse;
import com.payflow.gateway.common.security.MerchantPrincipal;
import com.payflow.gateway.ledger.dto.JournalEntryResponse;
import com.payflow.gateway.ledger.repository.JournalEntryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ledger")
public class LedgerController {

    private final JournalEntryRepository journalEntryRepository;

    public LedgerController(JournalEntryRepository journalEntryRepository) {
        this.journalEntryRepository = journalEntryRepository;
    }

    @GetMapping("/entries")
    public ResponseEntity<ApiResponse<List<JournalEntryResponse>>> getEntries(
        @AuthenticationPrincipal MerchantPrincipal principal
    ) {
        // Returns all journal entries - in production, would filter by merchant's payments/refunds
        List<JournalEntryResponse> entries = journalEntryRepository.findAllByOrderByCreatedAtDesc()
            .stream().map(JournalEntryResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.success(entries));
    }

    @GetMapping("/entries/{referenceId}")
    public ResponseEntity<ApiResponse<List<JournalEntryResponse>>> getEntriesByReference(
        @AuthenticationPrincipal MerchantPrincipal principal,
        @PathVariable UUID referenceId
    ) {
        List<JournalEntryResponse> entries = journalEntryRepository
            .findByReferenceIdOrderByCreatedAtDesc(referenceId)
            .stream().map(JournalEntryResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.success(entries));
    }
}

