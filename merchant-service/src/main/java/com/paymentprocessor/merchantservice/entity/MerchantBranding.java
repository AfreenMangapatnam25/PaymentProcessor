package com.paymentprocessor.merchantservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Merchant checkout/receipt branding (one row per merchant). */
@Entity
@Table(name = "merchant_branding", indexes = {
        @Index(name = "ux_branding_merchant", columnList = "merchant_id", unique = true)
})
@Getter
@Setter
public class MerchantBranding extends BaseEntity {

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "logo_url", length = 1024)
    private String logoUrl;

    @Column(name = "primary_color", length = 9)
    private String primaryColor;

    @Column(name = "secondary_color", length = 9)
    private String secondaryColor;

    /** How the business name appears on customer card statements. */
    @Column(name = "statement_descriptor", length = 22)
    private String statementDescriptor;

    @Column(name = "custom_domain", length = 255)
    private String customDomain;

    @Column(name = "email_template_ref", length = 255)
    private String emailTemplateRef;
}
