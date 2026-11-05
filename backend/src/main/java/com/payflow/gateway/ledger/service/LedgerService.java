package com.payflow.gateway.ledger.service;

import com.payflow.gateway.common.exception.BusinessRuleException;
import com.payflow.gateway.ledger.entity.JournalEntry;
import com.payflow.gateway.ledger.entity.LedgerAccount;
import com.payflow.gateway.ledger.entity.LedgerLine;
import com.payflow.gateway.ledger.repository.JournalEntryRepository;
import com.payflow.gateway.ledger.repository.LedgerAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Double-entry bookkeeping service.
 * Every financial posting must have balanced debits and credits.
 */
@Service
public class LedgerService {

    private final JournalEntryRepository journalEntryRepository;
    private final LedgerAccountRepository ledgerAccountRepository;

    public LedgerService(JournalEntryRepository journalEntryRepository,
                         LedgerAccountRepository ledgerAccountRepository) {
        this.journalEntryRepository = journalEntryRepository;
        this.ledgerAccountRepository = ledgerAccountRepository;
    }

    /**
     * Post a payment capture journal entry.
     * Debit PAYMENT_CLEARING, Credit MERCHANT_PAYABLE
     * Debit MERCHANT_PAYABLE (fee), Credit GATEWAY_FEE_REVENUE
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public JournalEntry postPaymentCapture(UUID paymentId, Long amountMinor, String currency, Long feeMinor) {
        if (journalEntryRepository.existsByReferenceId(paymentId)) {
            // Idempotent - already posted
            return journalEntryRepository.findByReferenceIdOrderByCreatedAtDesc(paymentId)
                .stream().findFirst()
                .orElseThrow(() -> new BusinessRuleException("Ledger entry not found", "LEDGER_ERROR"));
        }

        LedgerAccount paymentClearing = getSystemAccount("PAYMENT_CLEARING", currency);
        LedgerAccount merchantPayable = getSystemAccount("MERCHANT_PAYABLE", currency);
        LedgerAccount gatewayFeeRevenue = getSystemAccount("GATEWAY_FEE_REVENUE", currency);

        JournalEntry entry = new JournalEntry();
        entry.setReferenceType("PAYMENT");
        entry.setReferenceId(paymentId);
        entry.setDescription("Payment capture: " + amountMinor + " " + currency);
        entry.setStatus("POSTED");

        // Line 1: Debit PAYMENT_CLEARING (money received from customer)
        LedgerLine debit1 = createLine(entry, paymentClearing, "DEBIT", amountMinor, currency);

        // Line 2: Credit MERCHANT_PAYABLE (owed to merchant)
        Long netAmount = amountMinor - feeMinor;
        LedgerLine credit1 = createLine(entry, merchantPayable, "CREDIT", netAmount, currency);

        // Line 3: Debit MERCHANT_PAYABLE (fee deducted from merchant payable)
        LedgerLine debit2 = createLine(entry, merchantPayable, "DEBIT", feeMinor, currency);

        // Line 4: Credit GATEWAY_FEE_REVENUE (fee earned by gateway)
        LedgerLine credit2 = createLine(entry, gatewayFeeRevenue, "CREDIT", feeMinor, currency);

        entry.getLines().add(debit1);
        entry.getLines().add(credit1);
        entry.getLines().add(debit2);
        entry.getLines().add(credit2);

        validateBalance(entry);
        return journalEntryRepository.save(entry);
    }

    /**
     * Post a refund journal entry.
     * Debit MERCHANT_PAYABLE, Credit REFUND_CLEARING
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public JournalEntry postRefund(UUID refundId, Long amountMinor, String currency) {
        LedgerAccount merchantPayable = getSystemAccount("MERCHANT_PAYABLE", currency);
        LedgerAccount refundClearing = getSystemAccount("REFUND_CLEARING", currency);

        JournalEntry entry = new JournalEntry();
        entry.setReferenceType("REFUND");
        entry.setReferenceId(refundId);
        entry.setDescription("Refund: " + amountMinor + " " + currency);
        entry.setStatus("POSTED");

        LedgerLine debit = createLine(entry, merchantPayable, "DEBIT", amountMinor, currency);
        LedgerLine credit = createLine(entry, refundClearing, "CREDIT", amountMinor, currency);

        entry.getLines().add(debit);
        entry.getLines().add(credit);

        validateBalance(entry);
        return journalEntryRepository.save(entry);
    }

    private LedgerLine createLine(JournalEntry entry, LedgerAccount account, String direction,
                                   Long amountMinor, String currency) {
        LedgerLine line = new LedgerLine();
        line.setJournalEntry(entry);
        line.setAccount(account);
        line.setDirection(direction);
        line.setAmountMinor(amountMinor);
        line.setCurrency(currency);
        return line;
    }

    private LedgerAccount getSystemAccount(String accountCode, String currency) {
        return ledgerAccountRepository
            .findByAccountCodeAndMerchantIdIsNullAndCurrency(accountCode, currency)
            .orElseThrow(() -> new BusinessRuleException(
                "System ledger account not found: " + accountCode + " " + currency,
                "LEDGER_ACCOUNT_NOT_FOUND"
            ));
    }

    private void validateBalance(JournalEntry entry) {
        long totalDebits = entry.getLines().stream()
            .filter(l -> "DEBIT".equals(l.getDirection()))
            .mapToLong(LedgerLine::getAmountMinor)
            .sum();

        long totalCredits = entry.getLines().stream()
            .filter(l -> "CREDIT".equals(l.getDirection()))
            .mapToLong(LedgerLine::getAmountMinor)
            .sum();

        if (totalDebits != totalCredits) {
            throw new BusinessRuleException(
                "Journal entry is not balanced. Debits: " + totalDebits + ", Credits: " + totalCredits,
                "LEDGER_IMBALANCED"
            );
        }
    }
}