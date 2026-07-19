package com.paymentprocessor.fraudservice.domain.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A fraud investigation case opened for a payment intent, tracked through its
 * review lifecycle by fraud analysts.
 */
@Entity
@Table(
        name = "cases",
        indexes = {
                @Index(name = "idx_cases_intent", columnList = "intent_id"),
                @Index(name = "idx_cases_merchant", columnList = "merchant_id"),
                @Index(name = "idx_cases_status", columnList = "status")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudCase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "merchant_id", length = 100)
    private String merchantId;

    @Column(name = "intent_id", length = 100)
    private String intentId;

    @Column(length = 30)
    private String status;

    @Column(length = 100)
    private String assignee;

    @Column(name = "created_at")
    private Instant createdAt;
}
