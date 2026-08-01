package com.paymentprocessor.auditservice.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strongly-typed configuration for the audit service, bound from the {@code audit.*}
 * namespace in application.yml. Grouped by concern.
 */
@ConfigurationProperties(prefix = "audit")
public class AuditProperties {

    private final Security security = new Security();
    private final Chain chain = new Chain();
    private final Ingestion ingestion = new Ingestion();
    private final Kafka kafka = new Kafka();
    private final Batch batch = new Batch();
    private final S3 s3 = new S3();
    private final Signing signing = new Signing();
    private final Anchor anchor = new Anchor();

    public Security getSecurity() { return security; }
    public Chain getChain() { return chain; }
    public Ingestion getIngestion() { return ingestion; }
    public Kafka getKafka() { return kafka; }
    public Batch getBatch() { return batch; }
    public S3 getS3() { return s3; }
    public Signing getSigning() { return signing; }
    public Anchor getAnchor() { return anchor; }

    public static class Security {
        /** Accepted internal API keys presented via the X-Api-Key header. */
        private List<String> apiKeys = List.of();
        /** When false, the API-key filter is disabled (local/dev only). */
        private boolean enabled = true;

        public List<String> getApiKeys() { return apiKeys; }
        public void setApiKeys(List<String> apiKeys) { this.apiKeys = apiKeys; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public static class Chain {
        /** prev_hash of the first record in the chain. */
        private String genesisHash =
                "sha256:0000000000000000000000000000000000000000000000000000000000000000";

        public String getGenesisHash() { return genesisHash; }
        public void setGenesisHash(String genesisHash) { this.genesisHash = genesisHash; }
    }

    public static class Ingestion {
        private boolean piiGuardEnabled = true;
        private int maxRetriesOnContention = 8;

        public boolean isPiiGuardEnabled() { return piiGuardEnabled; }
        public void setPiiGuardEnabled(boolean piiGuardEnabled) { this.piiGuardEnabled = piiGuardEnabled; }
        public int getMaxRetriesOnContention() { return maxRetriesOnContention; }
        public void setMaxRetriesOnContention(int v) { this.maxRetriesOnContention = v; }
    }

    public static class Kafka {
        private boolean enabled = true;
        private String topic = "auditservicetopic";
        private String dltTopic = "auditservicetopic.DLT";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getTopic() { return topic; }
        public void setTopic(String topic) { this.topic = topic; }
        public String getDltTopic() { return dltTopic; }
        public void setDltTopic(String dltTopic) { this.dltTopic = dltTopic; }
    }

    public static class Batch {
        private boolean enabled = true;
        private String cron = "0 30 0 * * *";
        private String zone = "UTC";
        private int retentionYears = 10;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getCron() { return cron; }
        public void setCron(String cron) { this.cron = cron; }
        public String getZone() { return zone; }
        public void setZone(String zone) { this.zone = zone; }
        public int getRetentionYears() { return retentionYears; }
        public void setRetentionYears(int retentionYears) { this.retentionYears = retentionYears; }
    }

    public static class S3 {
        private boolean enabled = true;
        private String bucket = "payment-processor-audit-legal";
        private String region = "us-east-1";
        private String endpoint = "";
        private String keyPrefix = "audit-batches";
        private String objectLockMode = "COMPLIANCE";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getBucket() { return bucket; }
        public void setBucket(String bucket) { this.bucket = bucket; }
        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
        public String getKeyPrefix() { return keyPrefix; }
        public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }
        public String getObjectLockMode() { return objectLockMode; }
        public void setObjectLockMode(String objectLockMode) { this.objectLockMode = objectLockMode; }
    }

    public static class Signing {
        private String privateKeyPkcs8 = "";
        private String keyId = "audit-batch-signer-v1";

        public String getPrivateKeyPkcs8() { return privateKeyPkcs8; }
        public void setPrivateKeyPkcs8(String privateKeyPkcs8) { this.privateKeyPkcs8 = privateKeyPkcs8; }
        public String getKeyId() { return keyId; }
        public void setKeyId(String keyId) { this.keyId = keyId; }
    }

    public static class Anchor {
        /** "log" or "http". */
        private String mode = "log";
        private String endpoint = "";

        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }
        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    }
}
