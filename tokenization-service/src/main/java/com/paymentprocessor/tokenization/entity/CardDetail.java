package com.paymentprocessor.tokenization.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "card_details")
public class CardDetail {

    @Id
    @Column(name = "instrument_id")
    private String instrumentId;

    @Column(name = "pan_ciphertext")
    private byte[] panCiphertext;

    @Column(name = "dek_id")
    private String dekId;

    @Column(name = "nonce")
    private byte[] nonce;

    @Column(name = "aad")
    private byte[] aad;

    @Column(name = "exp_month")
    private Short expMonth;

    @Column(name = "exp_year")
    private Short expYear;

    @Column(name = "last4")
    private String last4;

    @Column(name = "bin")
    private String bin;

    @Column(name = "brand")
    private String brand;

    @Column(name = "funding")
    private String funding;

    @Column(name = "issuer_country")
    private String issuerCountry;

    @Column(name = "product_code")
    private String productCode;

    @Column(name = "cardholder_name_ciphertext")
    private byte[] cardholderNameCiphertext;

    public String getInstrumentId() { return instrumentId; }
    public void setInstrumentId(String instrumentId) { this.instrumentId = instrumentId; }
    public byte[] getPanCiphertext() { return panCiphertext; }
    public void setPanCiphertext(byte[] panCiphertext) { this.panCiphertext = panCiphertext; }
    public String getDekId() { return dekId; }
    public void setDekId(String dekId) { this.dekId = dekId; }
    public byte[] getNonce() { return nonce; }
    public void setNonce(byte[] nonce) { this.nonce = nonce; }
    public byte[] getAad() { return aad; }
    public void setAad(byte[] aad) { this.aad = aad; }
    public Short getExpMonth() { return expMonth; }
    public void setExpMonth(Short expMonth) { this.expMonth = expMonth; }
    public Short getExpYear() { return expYear; }
    public void setExpYear(Short expYear) { this.expYear = expYear; }
    public String getLast4() { return last4; }
    public void setLast4(String last4) { this.last4 = last4; }
    public String getBin() { return bin; }
    public void setBin(String bin) { this.bin = bin; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getFunding() { return funding; }
    public void setFunding(String funding) { this.funding = funding; }
    public String getIssuerCountry() { return issuerCountry; }
    public void setIssuerCountry(String issuerCountry) { this.issuerCountry = issuerCountry; }
    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }
    public byte[] getCardholderNameCiphertext() { return cardholderNameCiphertext; }
    public void setCardholderNameCiphertext(byte[] cardholderNameCiphertext) { this.cardholderNameCiphertext = cardholderNameCiphertext; }
}
