package com.paymentprocessor.fraudservice.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Real-time fraud evaluation request submitted by the Payment Service (or
 * Authentication Service for login events) before authorization.
 *
 * <p>All fields are optional so the engine degrades gracefully when an upstream
 * signal is missing. Absent signals simply contribute no risk rather than
 * failing the evaluation.
 */
public class FraudEvaluationRequest {

    // --- Transaction identity -------------------------------------------------
    private String intentId;
    private String merchantId;
    private String userId;
    private BigDecimal amount;
    private String currency;
    private Instant timestamp;

    // --- Card / BIN signals ---------------------------------------------------
    private String cardBin;
    private String cardLast4;
    private String cardFingerprint;
    private String cardType;        // e.g. CREDIT, DEBIT, PREPAID, VIRTUAL
    private String cardCurrency;
    private String binCountry;      // issuing country from BIN

    // --- Network / IP signals -------------------------------------------------
    private String ipAddress;
    private String ipCountry;
    private String ipType;          // e.g. RESIDENTIAL, VPN, PROXY, TOR, HOSTING
    private Boolean ipKnownFraud;

    // --- Geography ------------------------------------------------------------
    private String billingCountry;
    private Boolean sanctionedCountry;

    // --- Device signals -------------------------------------------------------
    private String deviceFingerprint;
    private Boolean deviceNew;      // first time seen for this user
    private Boolean emulator;
    private Boolean rooted;
    private Boolean vpn;

    // --- Identity / email -----------------------------------------------------
    private String email;
    private String emailDomain;
    private Boolean disposableEmail;

    // --- User risk ------------------------------------------------------------
    private Integer userAccountAgeDays;
    private String userKycStatus;   // e.g. VERIFIED, PENDING, FAILED, UNVERIFIED
    private Integer userDisputeCount;
    private Boolean userNewCustomer;

    // --- Merchant risk --------------------------------------------------------
    private Integer merchantAccountAgeDays;
    private Double merchantChargebackRate; // ratio, e.g. 0.012 == 1.2%
    private String merchantMcc;
    private Boolean merchantHighRisk;

    // --- AML / compliance signals (may be pre-enriched upstream) --------------
    private Boolean sanctionsHit;
    private Boolean pepHit;

    public String getIntentId() { return intentId; }
    public void setIntentId(String intentId) { this.intentId = intentId; }
    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public String getCardBin() { return cardBin; }
    public void setCardBin(String cardBin) { this.cardBin = cardBin; }
    public String getCardLast4() { return cardLast4; }
    public void setCardLast4(String cardLast4) { this.cardLast4 = cardLast4; }
    public String getCardFingerprint() { return cardFingerprint; }
    public void setCardFingerprint(String cardFingerprint) { this.cardFingerprint = cardFingerprint; }
    public String getCardType() { return cardType; }
    public void setCardType(String cardType) { this.cardType = cardType; }
    public String getCardCurrency() { return cardCurrency; }
    public void setCardCurrency(String cardCurrency) { this.cardCurrency = cardCurrency; }
    public String getBinCountry() { return binCountry; }
    public void setBinCountry(String binCountry) { this.binCountry = binCountry; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getIpCountry() { return ipCountry; }
    public void setIpCountry(String ipCountry) { this.ipCountry = ipCountry; }
    public String getIpType() { return ipType; }
    public void setIpType(String ipType) { this.ipType = ipType; }
    public Boolean getIpKnownFraud() { return ipKnownFraud; }
    public void setIpKnownFraud(Boolean ipKnownFraud) { this.ipKnownFraud = ipKnownFraud; }
    public String getBillingCountry() { return billingCountry; }
    public void setBillingCountry(String billingCountry) { this.billingCountry = billingCountry; }
    public Boolean getSanctionedCountry() { return sanctionedCountry; }
    public void setSanctionedCountry(Boolean sanctionedCountry) { this.sanctionedCountry = sanctionedCountry; }
    public String getDeviceFingerprint() { return deviceFingerprint; }
    public void setDeviceFingerprint(String deviceFingerprint) { this.deviceFingerprint = deviceFingerprint; }
    public Boolean getDeviceNew() { return deviceNew; }
    public void setDeviceNew(Boolean deviceNew) { this.deviceNew = deviceNew; }
    public Boolean getEmulator() { return emulator; }
    public void setEmulator(Boolean emulator) { this.emulator = emulator; }
    public Boolean getRooted() { return rooted; }
    public void setRooted(Boolean rooted) { this.rooted = rooted; }
    public Boolean getVpn() { return vpn; }
    public void setVpn(Boolean vpn) { this.vpn = vpn; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getEmailDomain() { return emailDomain; }
    public void setEmailDomain(String emailDomain) { this.emailDomain = emailDomain; }
    public Boolean getDisposableEmail() { return disposableEmail; }
    public void setDisposableEmail(Boolean disposableEmail) { this.disposableEmail = disposableEmail; }
    public Integer getUserAccountAgeDays() { return userAccountAgeDays; }
    public void setUserAccountAgeDays(Integer userAccountAgeDays) { this.userAccountAgeDays = userAccountAgeDays; }
    public String getUserKycStatus() { return userKycStatus; }
    public void setUserKycStatus(String userKycStatus) { this.userKycStatus = userKycStatus; }
    public Integer getUserDisputeCount() { return userDisputeCount; }
    public void setUserDisputeCount(Integer userDisputeCount) { this.userDisputeCount = userDisputeCount; }
    public Boolean getUserNewCustomer() { return userNewCustomer; }
    public void setUserNewCustomer(Boolean userNewCustomer) { this.userNewCustomer = userNewCustomer; }
    public Integer getMerchantAccountAgeDays() { return merchantAccountAgeDays; }
    public void setMerchantAccountAgeDays(Integer merchantAccountAgeDays) { this.merchantAccountAgeDays = merchantAccountAgeDays; }
    public Double getMerchantChargebackRate() { return merchantChargebackRate; }
    public void setMerchantChargebackRate(Double merchantChargebackRate) { this.merchantChargebackRate = merchantChargebackRate; }
    public String getMerchantMcc() { return merchantMcc; }
    public void setMerchantMcc(String merchantMcc) { this.merchantMcc = merchantMcc; }
    public Boolean getMerchantHighRisk() { return merchantHighRisk; }
    public void setMerchantHighRisk(Boolean merchantHighRisk) { this.merchantHighRisk = merchantHighRisk; }
    public Boolean getSanctionsHit() { return sanctionsHit; }
    public void setSanctionsHit(Boolean sanctionsHit) { this.sanctionsHit = sanctionsHit; }
    public Boolean getPepHit() { return pepHit; }
    public void setPepHit(Boolean pepHit) { this.pepHit = pepHit; }
}
