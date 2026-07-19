package com.paymentprocessor.authorization.domain.payment;

import com.paymentprocessor.authorization.domain.enums.AuthorizationStatus;
import com.paymentprocessor.authorization.domain.enums.AuthorizationType;
import com.paymentprocessor.authorization.domain.enums.AvsResult;
import com.paymentprocessor.authorization.domain.enums.CardNetwork;
import com.paymentprocessor.authorization.domain.enums.CvvResult;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Authoritative record of a payment authorization and its lifecycle.
 *
 * <p>PCI note: only network-safe card metadata (BIN, last four, brand, expiry) is persisted.
 * The full PAN and CVV are never stored.
 */
@Entity
@Table(name = "authorization_record", indexes = {
        @Index(name = "idx_auth_merchant", columnList = "merchant_id"),
        @Index(name = "idx_auth_payment_ref", columnList = "payment_reference"),
        @Index(name = "idx_auth_status", columnList = "status"),
        @Index(name = "idx_auth_idempotency", columnList = "idempotency_key"),
        @Index(name = "idx_auth_gateway_id", columnList = "gateway_authorization_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizationRecord {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AuthorizationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private AuthorizationType type;

    @Column(name = "merchant_id", nullable = false, length = 64)
    private String merchantId;

    @Column(name = "customer_id", length = 64)
    private String customerId;

    /** Payment / transaction id owned by the Payment Service. */
    @Column(name = "payment_reference", nullable = false, length = 64)
    private String paymentReference;

    @Column(name = "requested_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal requestedAmount;

    @Column(name = "approved_amount", precision = 19, scale = 4)
    private BigDecimal approvedAmount;

    @Column(name = "captured_amount", precision = 19, scale = 4)
    private BigDecimal capturedAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_network", length = 16)
    private CardNetwork cardNetwork;

    @Column(name = "card_bin", length = 8)
    private String cardBin;

    @Column(name = "card_last4", length = 4)
    private String cardLast4;

    @Column(name = "card_exp_month")
    private Integer cardExpMonth;

    @Column(name = "card_exp_year")
    private Integer cardExpYear;

    /** Issuer authorization code. */
    @Column(name = "authorization_code", length = 32)
    private String authorizationCode;

    /** Network / scheme reference id for downstream reconciliation. */
    @Column(name = "network_reference_id", length = 64)
    private String networkReferenceId;

    @Column(name = "gateway_provider", length = 32)
    private String gatewayProvider;

    @Column(name = "gateway_authorization_id", length = 128)
    private String gatewayAuthorizationId;

    @Column(name = "gateway_response_code", length = 32)
    private String gatewayResponseCode;

    @Column(name = "gateway_response_message", length = 512)
    private String gatewayResponseMessage;

    /** Normalized issuer response/decline code. */
    @Column(name = "response_code", length = 32)
    private String responseCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "avs_result", length = 16)
    private AvsResult avsResult;

    @Enumerated(EnumType.STRING)
    @Column(name = "cvv_result", length = 16)
    private CvvResult cvvResult;

    @Column(name = "requires_authentication", nullable = false)
    private boolean requiresAuthentication;

    /** 3-D Secure redirect / challenge URL when authentication is required. */
    @Column(name = "authentication_url", length = 1024)
    private String authenticationUrl;

    /** Links a re-authorization back to the original authorization. */
    @Column(name = "original_authorization_id")
    private UUID originalAuthorizationId;

    @Column(name = "reauthorization_count", nullable = false)
    private int reauthorizationCount;

    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Column(name = "risk_score")
    private Double riskScore;

    /** Raw gateway response payload retained for audit / dispute handling. */
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "gateway_raw_response")
    private String gatewayRawResponse;

    /** When the authorization hold expires if not captured. */
    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "authorized_at")
    private Instant authorizedAt;

    @Column(name = "captured_at")
    private Instant capturedAt;

    @Column(name = "reversed_at")
    private Instant reversedAt;

    @Version
    @Column(nullable = false)
    private long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
