package com.paymentprocessor.ledgerservice.entity;

import com.paymentprocessor.ledgerservice.domain.enums.AccountClassification;
import com.paymentprocessor.ledgerservice.domain.enums.NormalBalance;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Reference data describing an account type: its accounting classification and
 * the side on which its balance normally increases.
 */
@Entity
@Table(name = "account_types")
public class AccountType {

    @Id
    @Column(name = "code", length = 16)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "classification", length = 16, nullable = false)
    private AccountClassification classification;

    @Enumerated(EnumType.STRING)
    @Column(name = "normal_balance", length = 8, nullable = false)
    private NormalBalance normalBalance;

    @Column(name = "description")
    private String description;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public AccountClassification getClassification() { return classification; }
    public void setClassification(AccountClassification classification) { this.classification = classification; }
    public NormalBalance getNormalBalance() { return normalBalance; }
    public void setNormalBalance(NormalBalance normalBalance) { this.normalBalance = normalBalance; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
