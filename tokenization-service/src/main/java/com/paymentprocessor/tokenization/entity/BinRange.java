package com.paymentprocessor.tokenization.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "bin_ranges")
public class BinRange {

    @Id
    private Long id;

    @Column(name = "bin_low")
    private Long binLow;

    @Column(name = "bin_high")
    private Long binHigh;

    @Column(name = "brand")
    private String brand;

    @Column(name = "funding")
    private String funding;

    @Column(name = "issuer")
    private String issuer;

    @Column(name = "country")
    private String country;

    @Column(name = "product_code")
    private String productCode;

    @Column(name = "is_prepaid")
    private Boolean isPrepaid;

    @Column(name = "is_commercial")
    private Boolean isCommercial;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBinLow() { return binLow; }
    public void setBinLow(Long binLow) { this.binLow = binLow; }
    public Long getBinHigh() { return binHigh; }
    public void setBinHigh(Long binHigh) { this.binHigh = binHigh; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getFunding() { return funding; }
    public void setFunding(String funding) { this.funding = funding; }
    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }
    public Boolean getIsPrepaid() { return isPrepaid; }
    public void setIsPrepaid(Boolean isPrepaid) { this.isPrepaid = isPrepaid; }
    public Boolean getIsCommercial() { return isCommercial; }
    public void setIsCommercial(Boolean isCommercial) { this.isCommercial = isCommercial; }
}
