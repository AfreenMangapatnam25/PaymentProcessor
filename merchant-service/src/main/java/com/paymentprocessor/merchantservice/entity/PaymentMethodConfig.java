package com.paymentprocessor.merchantservice.entity;

import com.paymentprocessor.merchantservice.common.enums.PaymentMethodType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Enablement and method-specific settings for a payment instrument a merchant can accept. */
@Entity
@Table(name = "payment_method_config",
        uniqueConstraints = @UniqueConstraint(name = "ux_pmconfig_merchant_method",
                columnNames = {"merchant_id", "method_type"}),
        indexes = @Index(name = "ix_pmconfig_merchant", columnList = "merchant_id"))
@Getter
@Setter
public class PaymentMethodConfig extends BaseEntity {

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "method_type", nullable = false, length = 20)
    private PaymentMethodType methodType;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = false;

    /** Free-form JSON of method-specific settings, e.g. {"threeDS":"required"}. */
    @Column(name = "settings_json", columnDefinition = "text")
    private String settingsJson;
}
