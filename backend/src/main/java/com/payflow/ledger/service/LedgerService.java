package com.payflow.ledger.service;

import com.payflow.common.dto.PageResponse;
import com.payflow.common.enums.LedgerAccountType;
import com.payflow.common.enums.LedgerEntryType;
import com.payflow.common.exception.PayFlowException;
import com.payflow.ledger.entity.LedgerAccount;
import com.payflow.ledger.entity.LedgerEntry;
import com.payflow.ledger.repository.LedgerAccountRepository;
import com.payflow.ledger.repository.LedgerEntryRepository;
import com.payflow.merchant.service.MerchantService;
import com.payflow.payment.entity.Payment;
import com.payflow.transaction.entity.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LedgerService {

    private final LedgerAccountRepository accountRepository;
    private final LedgerEntryRepository entryRepository;
    private final MerchantService merchantService;

    @Transactional
    public void postPaymentCompleted(Payment payment, Transaction transaction) {
        LedgerAccount customerPayable = getOrCreateCustomerPayable(payment.getCustomerId(), payment.getCurrency());
        LedgerAccount merchantReceivable = getOrCreateMerchantReceivable(payment.getMerchantId(), payment.getCurrency());

        long amount = payment.getAmountCents();

        // Double-entry: DEBIT customer_payable + CREDIT merchant_receivable
        postEntry(customerPayable, LedgerEntryType.DEBIT, amount, payment.getCurrency(),
                payment.getId(), transaction.getId(), "Payment debit customer payable");
        postEntry(merchantReceivable, LedgerEntryType.CREDIT, amount, payment.getCurrency(),
                payment.getId(), transaction.getId(), "Payment credit merchant receivable");

        assertBalanced(amount, amount);
    }

    @Transactional
    public void postRefund(Payment payment, Transaction transaction, long refundAmount) {
        LedgerAccount customerPayable = getOrCreateCustomerPayable(payment.getCustomerId(), payment.getCurrency());
        LedgerAccount merchantReceivable = getOrCreateMerchantReceivable(payment.getMerchantId(), payment.getCurrency());

        // Reverse: CREDIT customer_payable + DEBIT merchant_receivable
        postEntry(merchantReceivable, LedgerEntryType.DEBIT, refundAmount, payment.getCurrency(),
                payment.getId(), transaction.getId(), "Refund debit merchant receivable");
        postEntry(customerPayable, LedgerEntryType.CREDIT, refundAmount, payment.getCurrency(),
                payment.getId(), transaction.getId(), "Refund credit customer payable");

        assertBalanced(refundAmount, refundAmount);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAccounts(UUID merchantId) {
        List<LedgerAccount> accounts;
        if (merchantId != null) {
            merchantService.assertCanAccess(merchantId);
            accounts = accountRepository.findByMerchantId(merchantId);
        } else {
            accounts = accountRepository.findAll();
        }
        return accounts.stream().map(this::toAccountMap).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listEntries(UUID accountId, Pageable pageable) {
        LedgerAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> PayFlowException.notFound("Ledger account not found"));
        if (account.getMerchantId() != null) {
            merchantService.assertCanAccess(account.getMerchantId());
        }
        return PageResponse.from(entryRepository.findByAccountId(accountId, pageable).map(this::toEntryMap));
    }

    public LedgerAccount getOrCreateMerchantReceivable(UUID merchantId, String currency) {
        return accountRepository.findByMerchantIdAndAccountType(merchantId, LedgerAccountType.MERCHANT_RECEIVABLE)
                .orElseGet(() -> accountRepository.save(LedgerAccount.builder()
                        .merchantId(merchantId)
                        .accountType(LedgerAccountType.MERCHANT_RECEIVABLE)
                        .currency(currency)
                        .name("Merchant receivable")
                        .balanceCents(0)
                        .build()));
    }

    public LedgerAccount getOrCreateCustomerPayable(UUID customerId, String currency) {
        if (customerId == null) {
            throw PayFlowException.badRequest("Customer required for ledger posting");
        }
        return accountRepository.findByCustomerIdAndAccountType(customerId, LedgerAccountType.CUSTOMER_PAYABLE)
                .orElseGet(() -> accountRepository.save(LedgerAccount.builder()
                        .customerId(customerId)
                        .accountType(LedgerAccountType.CUSTOMER_PAYABLE)
                        .currency(currency)
                        .name("Customer payable")
                        .balanceCents(0)
                        .build()));
    }

    private void postEntry(LedgerAccount account, LedgerEntryType type, long amount, String currency,
                           UUID paymentId, UUID transactionId, String description) {
        if (type == LedgerEntryType.DEBIT) {
            account.setBalanceCents(account.getBalanceCents() - amount);
        } else {
            account.setBalanceCents(account.getBalanceCents() + amount);
        }
        accountRepository.save(account);

        entryRepository.save(LedgerEntry.builder()
                .accountId(account.getId())
                .paymentId(paymentId)
                .transactionId(transactionId)
                .entryType(type)
                .amountCents(amount)
                .currency(currency)
                .description(description)
                .build());
    }

    private void assertBalanced(long debitTotal, long creditTotal) {
        if (debitTotal != creditTotal) {
            throw PayFlowException.badRequest("Unbalanced ledger entry");
        }
    }

    private Map<String, Object> toAccountMap(LedgerAccount a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("merchantId", a.getMerchantId());
        m.put("customerId", a.getCustomerId());
        m.put("accountType", a.getAccountType());
        m.put("currency", a.getCurrency());
        m.put("name", a.getName());
        m.put("balanceCents", a.getBalanceCents());
        m.put("createdAt", a.getCreatedAt());
        return m;
    }

    private Map<String, Object> toEntryMap(LedgerEntry e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("accountId", e.getAccountId());
        m.put("paymentId", e.getPaymentId());
        m.put("transactionId", e.getTransactionId());
        m.put("entryType", e.getEntryType());
        m.put("amountCents", e.getAmountCents());
        m.put("currency", e.getCurrency());
        m.put("description", e.getDescription());
        m.put("createdAt", e.getCreatedAt());
        return m;
    }
}
