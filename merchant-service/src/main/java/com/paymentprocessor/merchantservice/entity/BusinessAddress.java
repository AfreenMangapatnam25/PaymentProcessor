package com.paymentprocessor.merchantservice.entity;

import com.paymentprocessor.merchantservice.common.enums.AddressType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** A business address of a given type belonging to a merchant. */
@Entity
@Table(name = "business_address", indexes = {
        @Index(name = "ix_address_merchant", columnList = "merchant_id")
})
@Getter
@Setter
public class BusinessAddress extends BaseEntity {

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "address_type", nullable = false, length = 20)
    private AddressType addressType;

    @Column(name = "line1", nullable = false, length = 255)
    private String line1;

    @Column(name = "line2", length = 255)
    private String line2;

    @Column(name = "city", nullable = false, length = 120)
    private String city;

    @Column(name = "region", length = 120)
    private String region;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "country", nullable = false, length = 2)
    private String country;

    @Column(name = "is_primary", nullable = false)
    private boolean primary = false;
}
