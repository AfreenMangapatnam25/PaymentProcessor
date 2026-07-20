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
 * A blacklist/whitelist entry ("list" holds the kind, e.g. BLACKLIST or
 * WHITELIST) matched against a request attribute (CARD, IP, USER, DEVICE,
 * EMAIL_DOMAIN, MERCHANT) during evaluation.
 */
@Entity
@Table(
        name = "lists",
        indexes = {
                @Index(name = "idx_lists_attribute_value", columnList = "attribute,value"),
                @Index(name = "idx_lists_merchant", columnList = "merchant_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "merchant_id", length = 100)
    private String merchantId;

    /** Kind of list this entry belongs to, e.g. BLACKLIST or WHITELIST. */
    @Column(name = "list_kind", length = 20)
    private String list;

    @Column(length = 40)
    private String attribute;

    @Column(length = 200)
    private String value;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "expires_at")
    private Instant expiresAt;
}
