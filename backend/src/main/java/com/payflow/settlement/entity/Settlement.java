package com.payflow.settlement.entity;

import com.payflow.common.enums.SettlementStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "settlements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(nullable = false, unique = true, length = 64)
    private String reference;

    @Column(name = "gross_amount_cents", nullable = false)
    private long grossAmountCents;

    @Column(name = "fee_amount_cents", nullable = false)
    @Builder.Default
    private long feeAmountCents = 0;

    @Column(name = "net_amount_cents", nullable = false)
    private long netAmountCents;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";

    @Column(name = "transaction_count", nullable = false)
    @Builder.Default
    private int transactionCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private SettlementStatus status = SettlementStatus.PENDING;

    @Column(name = "settlement_date")
    private LocalDate settlementDate;

    @Column(name = "processed_at")
    private Instant processedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
