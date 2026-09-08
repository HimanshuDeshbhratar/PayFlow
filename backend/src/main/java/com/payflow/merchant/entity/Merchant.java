package com.payflow.merchant.entity;

import com.payflow.common.enums.MerchantSettlementStatus;
import com.payflow.common.enums.MerchantStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "merchants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "business_name", nullable = false)
    private String businessName;

    @Column(name = "legal_name")
    private String legalName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private MerchantStatus status = MerchantStatus.ACTIVE;

    @Column(name = "country_code", nullable = false, length = 3)
    @Builder.Default
    private String countryCode = "US";

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_status", nullable = false, length = 50)
    @Builder.Default
    private MerchantSettlementStatus settlementStatus = MerchantSettlementStatus.CURRENT;

    @Column(name = "webhook_secret")
    private String webhookSecret;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
