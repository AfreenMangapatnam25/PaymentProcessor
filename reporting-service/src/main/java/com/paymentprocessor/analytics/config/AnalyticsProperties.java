package com.paymentprocessor.analytics.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/** Strongly-typed binding for all {@code analytics.*} configuration. */
@ConfigurationProperties(prefix = "analytics")
public class AnalyticsProperties {

    @NestedConfigurationProperty
    private ClickHouse clickhouse = new ClickHouse();
    @NestedConfigurationProperty
    private Storage storage = new Storage();
    @NestedConfigurationProperty
    private Reports reports = new Reports();
    @NestedConfigurationProperty
    private Notifications notifications = new Notifications();

    public ClickHouse getClickhouse() { return clickhouse; }
    public Storage getStorage() { return storage; }
    public Reports getReports() { return reports; }
    public Notifications getNotifications() { return notifications; }

    public static class ClickHouse {
        private String url;
        private String username = "default";
        private String password = "";
        private int maxPoolSize = 8;
        private int queryTimeoutSeconds = 60;
        private long maxResultRows = 1_000_000;

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public int getMaxPoolSize() { return maxPoolSize; }
        public void setMaxPoolSize(int maxPoolSize) { this.maxPoolSize = maxPoolSize; }
        public int getQueryTimeoutSeconds() { return queryTimeoutSeconds; }
        public void setQueryTimeoutSeconds(int v) { this.queryTimeoutSeconds = v; }
        public long getMaxResultRows() { return maxResultRows; }
        public void setMaxResultRows(long v) { this.maxResultRows = v; }
    }

    public static class Storage {
        private String backend = "local";
        private String localDir = "/tmp/analytics-reports";
        private S3 s3 = new S3();

        public String getBackend() { return backend; }
        public void setBackend(String backend) { this.backend = backend; }
        public String getLocalDir() { return localDir; }
        public void setLocalDir(String localDir) { this.localDir = localDir; }
        public S3 getS3() { return s3; }
        public void setS3(S3 s3) { this.s3 = s3; }

        public static class S3 {
            private String bucket;
            private String region = "us-east-1";
            private String endpoint = "";
            private int presignTtlMinutes = 60;

            public String getBucket() { return bucket; }
            public void setBucket(String bucket) { this.bucket = bucket; }
            public String getRegion() { return region; }
            public void setRegion(String region) { this.region = region; }
            public String getEndpoint() { return endpoint; }
            public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
            public int getPresignTtlMinutes() { return presignTtlMinutes; }
            public void setPresignTtlMinutes(int v) { this.presignTtlMinutes = v; }
        }
    }

    public static class Reports {
        private Executor executor = new Executor();
        private int retentionDays = 30;
        private int maxConcurrentPerMerchant = 5;

        public Executor getExecutor() { return executor; }
        public void setExecutor(Executor executor) { this.executor = executor; }
        public int getRetentionDays() { return retentionDays; }
        public void setRetentionDays(int retentionDays) { this.retentionDays = retentionDays; }
        public int getMaxConcurrentPerMerchant() { return maxConcurrentPerMerchant; }
        public void setMaxConcurrentPerMerchant(int v) { this.maxConcurrentPerMerchant = v; }

        public static class Executor {
            private int corePoolSize = 4;
            private int maxPoolSize = 8;
            private int queueCapacity = 200;

            public int getCorePoolSize() { return corePoolSize; }
            public void setCorePoolSize(int v) { this.corePoolSize = v; }
            public int getMaxPoolSize() { return maxPoolSize; }
            public void setMaxPoolSize(int v) { this.maxPoolSize = v; }
            public int getQueueCapacity() { return queueCapacity; }
            public void setQueueCapacity(int v) { this.queueCapacity = v; }
        }
    }

    public static class Notifications {
        private String kafkaTopic = "report.completed";
        private Email email = new Email();

        public String getKafkaTopic() { return kafkaTopic; }
        public void setKafkaTopic(String kafkaTopic) { this.kafkaTopic = kafkaTopic; }
        public Email getEmail() { return email; }
        public void setEmail(Email email) { this.email = email; }

        public static class Email {
            private boolean enabled = true;
            private String from = "reports@paymentprocessor.com";

            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
            public String getFrom() { return from; }
            public void setFrom(String from) { this.from = from; }
        }
    }
}
