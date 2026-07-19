package com.paymentprocessor.disputeservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * Reference catalogue of network dispute reason codes. Keyed by (network, code),
 * each entry captures the human-readable description, the indicative win rate,
 * and the evidence categories that should be gathered to fight it. Used to guide
 * merchants and to check representment package completeness.
 */
@Entity
@Table(name = "reason_code_catalog")
@IdClass(ReasonCodeCatalogId.class)
public class ReasonCodeCatalog {

    @Id
    @Column(name = "network", length = 20)
    private String network;

    @Id
    @Column(name = "code", length = 20)
    private String code;

    @Column(name = "category", length = 60)
    private String category;

    @Column(name = "description", length = 200)
    private String description;

    /** Indicative win rate band: HIGH, MEDIUM or LOW. */
    @Column(name = "win_rate", length = 10)
    private String winRate;

    /** Comma-separated {@code EvidenceCategory} names that are required. */
    @Column(name = "required_evidence", length = 500)
    private String requiredEvidence;

    /** Comma-separated {@code EvidenceCategory} names that are optional. */
    @Column(name = "optional_evidence", length = 500)
    private String optionalEvidence;

    @Column(name = "response_days")
    private Integer responseDays;

    public ReasonCodeCatalog() {
    }

    public ReasonCodeCatalog(String network, String code, String category,
                             String description, String winRate,
                             String requiredEvidence, String optionalEvidence,
                             Integer responseDays) {
        this.network = network;
        this.code = code;
        this.category = category;
        this.description = description;
        this.winRate = winRate;
        this.requiredEvidence = requiredEvidence;
        this.optionalEvidence = optionalEvidence;
        this.responseDays = responseDays;
    }

    public String getNetwork() { return network; }
    public void setNetwork(String network) { this.network = network; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getWinRate() { return winRate; }
    public void setWinRate(String winRate) { this.winRate = winRate; }
    public String getRequiredEvidence() { return requiredEvidence; }
    public void setRequiredEvidence(String requiredEvidence) { this.requiredEvidence = requiredEvidence; }
    public String getOptionalEvidence() { return optionalEvidence; }
    public void setOptionalEvidence(String optionalEvidence) { this.optionalEvidence = optionalEvidence; }
    public Integer getResponseDays() { return responseDays; }
    public void setResponseDays(Integer responseDays) { this.responseDays = responseDays; }
}
