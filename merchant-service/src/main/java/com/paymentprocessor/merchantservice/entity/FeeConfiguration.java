package com.paymentprocessor.merchantservice.entity;

import com.paymentprocessor.merchantservice.common.enums.PricingPlan;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * The effective fee/pricing configuration for a merchant. Exactly one row per merchant is
 * marked active; historical rows are retained for audit.
 */
@Entity
@Table(name = "fee_configuration", indexes = {
        @Index(name = "ix_fee_merchant", columnList = "merchant_id")
})
@Getter
@Setter
public class FeeConfiguration extends BaseEntity {

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_plan", nullable = false, length = 20)
    private PricingPlan pricingPlan = PricingPlan.STANDARD;

    /** Percentage taken per transaction, e.g. 2.90 = 2.90%. */
    @Column(name = "transaction_fee_percent", precision = 6, scale = 3)
    private BigDecimal transactionFeePercent;

    /** Fixed minor-currency amount added per transaction. */
    @Column(name = "transaction_fee_fixed", precision = 12, scale = 2)
    private BigDecimal transactionFeeFixed;

    @Column(name = "interchange_pass_through", nullable = false)
    private boolean interchangePassThrough = false;

    @Column(name = "monthly_platform_fee", precision = 12, scale = 2)
    private BigDecimal monthlyPlatformFee;

    @Column(name = "chargeback_fee", precision = 12, scale = 2)
    private BigDecimal chargebackFee;

    @Column(name = "refund_fee", precision = 12, scale = 2)
    private BigDecimal refundFee;

    @Column(name = "payout_fee", precision = 12, scale = 2)
    private BigDecimal payoutFee;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
