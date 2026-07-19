package com.paymentprocessor.disputeservice.dto.request;

import com.paymentprocessor.disputeservice.domain.enums.DisputeSource;
import com.paymentprocessor.disputeservice.domain.enums.DisputeType;
import com.paymentprocessor.disputeservice.domain.enums.Network;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Command to open a new dispute from an inbound chargeback / retrieval
 * notification.
 */
public class CreateDisputeRequest {

    @NotBlank
    private String chargebackId;

    private String transactionId;
    private String paymentId;

    @NotBlank
    private String merchantId;

    private String customerId;

    @NotNull
    private Network network;

    @NotNull
    private DisputeType type;

    private DisputeSource source;

    @NotBlank
    private String reasonCode;

    private String reasonDescription;

    @NotNull
    @Positive
    private Long amountMinor;

    @NotBlank
    @Size(min = 3, max = 3)
    private String currency;

    private boolean partial;

    /** Merchant's current monthly chargeback rate (%), used for reserve tiering. */
    private double chargebackRatePercent;

    public String getChargebackId() { return chargebackId; }
    public void setChargebackId(String chargebackId) { this.chargebackId = chargebackId; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public Network getNetwork() { return network; }
    public void setNetwork(Network network) { this.network = network; }
    public DisputeType getType() { return type; }
    public void setType(DisputeType type) { this.type = type; }
    public DisputeSource getSource() { return source; }
    public void setSource(DisputeSource source) { this.source = source; }
    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }
    public String getReasonDescription() { return reasonDescription; }
    public void setReasonDescription(String reasonDescription) { this.reasonDescription = reasonDescription; }
    public Long getAmountMinor() { return amountMinor; }
    public void setAmountMinor(Long amountMinor) { this.amountMinor = amountMinor; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public boolean isPartial() { return partial; }
    public void setPartial(boolean partial) { this.partial = partial; }
    public double getChargebackRatePercent() { return chargebackRatePercent; }
    public void setChargebackRatePercent(double chargebackRatePercent) { this.chargebackRatePercent = chargebackRatePercent; }
}
