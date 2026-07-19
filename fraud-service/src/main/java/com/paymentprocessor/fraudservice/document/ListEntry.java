package com.paymentprocessor.fraudservice.document;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "lists")
public class ListEntry {

    @Id
    private String id;

    private String merchantId;
    private String list;
    private String attribute;
    private String value;
    private String reason;
    private Instant expiresAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
    public String getList() { return list; }
    public void setList(String list) { this.list = list; }
    public String getAttribute() { return attribute; }
    public void setAttribute(String attribute) { this.attribute = attribute; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
