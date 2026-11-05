package com.payflow.gateway.ledger.repository;

import com.payflow.gateway.ledger.entity.LedgerAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LedgerAccountRepository extends JpaRepository<LedgerAccount, UUID> {
    Optional<LedgerAccount> findByAccountCodeAndMerchantIdIsNullAndCurrency(String accountCode, String currency);
}

