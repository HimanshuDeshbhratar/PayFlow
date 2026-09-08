package com.payflow.ledger;

import com.payflow.common.enums.LedgerAccountType;
import com.payflow.common.enums.LedgerEntryType;
import com.payflow.ledger.entity.LedgerAccount;
import com.payflow.ledger.entity.LedgerEntry;
import com.payflow.ledger.repository.LedgerAccountRepository;
import com.payflow.ledger.repository.LedgerEntryRepository;
import com.payflow.ledger.service.LedgerService;
import com.payflow.merchant.service.MerchantService;
import com.payflow.payment.entity.Payment;
import com.payflow.transaction.entity.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    @Mock LedgerAccountRepository accountRepository;
    @Mock LedgerEntryRepository entryRepository;
    @Mock MerchantService merchantService;

    LedgerService service;

    @BeforeEach
    void setUp() {
        service = new LedgerService(accountRepository, entryRepository, merchantService);
    }

    @Test
    void postsDoubleEntryOnCompletedPayment() {
        UUID merchantId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();

        LedgerAccount customerPayable = LedgerAccount.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .accountType(LedgerAccountType.CUSTOMER_PAYABLE)
                .currency("USD")
                .name("Customer payable")
                .balanceCents(0)
                .build();
        LedgerAccount merchantReceivable = LedgerAccount.builder()
                .id(UUID.randomUUID())
                .merchantId(merchantId)
                .accountType(LedgerAccountType.MERCHANT_RECEIVABLE)
                .currency("USD")
                .name("Merchant receivable")
                .balanceCents(0)
                .build();

        when(accountRepository.findByCustomerIdAndAccountType(customerId, LedgerAccountType.CUSTOMER_PAYABLE))
                .thenReturn(Optional.of(customerPayable));
        when(accountRepository.findByMerchantIdAndAccountType(merchantId, LedgerAccountType.MERCHANT_RECEIVABLE))
                .thenReturn(Optional.of(merchantReceivable));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(entryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Payment payment = Payment.builder()
                .id(paymentId)
                .merchantId(merchantId)
                .customerId(customerId)
                .amountCents(5_000)
                .currency("USD")
                .reference("pay")
                .build();
        Transaction tx = Transaction.builder()
                .id(txId)
                .paymentId(paymentId)
                .merchantId(merchantId)
                .customerId(customerId)
                .amountCents(5_000)
                .currency("USD")
                .reference("txn")
                .build();

        service.postPaymentCompleted(payment, tx);

        assertThat(customerPayable.getBalanceCents()).isEqualTo(-5_000);
        assertThat(merchantReceivable.getBalanceCents()).isEqualTo(5_000);

        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(entryRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(LedgerEntry::getEntryType)
                .containsExactlyInAnyOrder(LedgerEntryType.DEBIT, LedgerEntryType.CREDIT);
        assertThat(captor.getAllValues()).allMatch(e -> e.getAmountCents() == 5_000);
    }
}
