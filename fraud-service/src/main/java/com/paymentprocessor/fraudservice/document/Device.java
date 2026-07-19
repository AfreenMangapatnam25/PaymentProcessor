package com.paymentprocessor.fraudservice.document;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "devices")
public class Device {

    @Id
    private String id;

    private String fingerprint;
    private String merchantId;
    private Instant firstSeen;
    private Instant lastSeen;
    private String metadata;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFingerprint() { return fingerprint; }
    public void setFingerprint(String fingerprint) { this.fingerprint = fingerprint; }
    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
    public Instant getFirstSeen() { return firstSeen; }
    public void setFirstSeen(Instant firstSeen) { this.firstSeen = firstSeen; }
    public Instant getLastSeen() { return lastSeen; }
    public void setLastSeen(Instant lastSeen) { this.lastSeen = lastSeen; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
}
