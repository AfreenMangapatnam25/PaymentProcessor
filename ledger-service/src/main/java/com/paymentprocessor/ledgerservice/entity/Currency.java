package com.paymentprocessor.ledgerservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Supported currency and its minor-unit exponent (e.g. USD -> 2, meaning cents).
 */
@Entity
@Table(name = "currencies")
public class Currency {

    @Id
    @Column(name = "code", length = 3)
    private String code;

    @Column(name = "exponent", nullable = false)
    private Short exponent;

    @Column(name = "name", length = 64)
    private String name;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Short getExponent() { return exponent; }
    public void setExponent(Short exponent) { this.exponent = exponent; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
