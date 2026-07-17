package com.paymentprocessor.tokenization.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "bank_details")
public class BankDetail {

    @Id
    @Column(name = "instrument_id")
    private String instrumentId;

    @Column(name = "account_ciphertext")
    private byte[] accountCiphertext;

    @Column(name = "routing_ciphertext")
    private byte[] routingCiphertext;

    @Column(name = "dek_id")
    private String dekId;

    @Column(name = "nonce")
    private byte[] nonce;

    @Column(name = "last4")
    private String last4;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "country")
    private String country;

    public String getInstrumentId() { return instrumentId; }
    public void setInstrumentId(String instrumentId) { this.instrumentId = instrumentId; }
    public byte[] getAccountCiphertext() { return accountCiphertext; }
    public void setAccountCiphertext(byte[] accountCiphertext) { this.accountCiphertext = accountCiphertext; }
    public byte[] getRoutingCiphertext() { return routingCiphertext; }
    public void setRoutingCiphertext(byte[] routingCiphertext) { this.routingCiphertext = routingCiphertext; }
    public String getDekId() { return dekId; }
    public void setDekId(String dekId) { this.dekId = dekId; }
    public byte[] getNonce() { return nonce; }
    public void setNonce(byte[] nonce) { this.nonce = nonce; }
    public String getLast4() { return last4; }
    public void setLast4(String last4) { this.last4 = last4; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
}
