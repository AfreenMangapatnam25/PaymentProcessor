package com.paymentprocessor.gatewayservice.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strongly-typed binding for the gateway's own tunables (prefix {@code gateway}).
 * Keeping these in one place makes the operational surface of the service explicit.
 */
@ConfigurationProperties(prefix = "gateway")
public class GatewayProperties {

    /** Reject request bodies larger than this many bytes before proxying. */
    private long maxRequestBodyBytes = 1_048_576L;

    /** Ant-style paths that bypass authentication (health, fallbacks, etc.). */
    private List<String> publicPaths = new ArrayList<>();

    private final Audit audit = new Audit();
    private final ApiKey apikey = new ApiKey();

    public long getMaxRequestBodyBytes() {
        return maxRequestBodyBytes;
    }

    public void setMaxRequestBodyBytes(long maxRequestBodyBytes) {
        this.maxRequestBodyBytes = maxRequestBodyBytes;
    }

    public List<String> getPublicPaths() {
        return publicPaths;
    }

    public void setPublicPaths(List<String> publicPaths) {
        this.publicPaths = publicPaths;
    }

    public Audit getAudit() {
        return audit;
    }

    public ApiKey getApikey() {
        return apikey;
    }

    /** Configuration for asynchronous audit event emission. */
    public static class Audit {
        private boolean enabled = true;
        private String uri = "http://audit-service:8080";
        private String path = "/api/v1/audit/events";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    /**
     * API key configuration. In production, keys should be loaded from a secret
     * manager and stored hashed; the in-config list here is for non-secret
     * environments and local development.
     */
    public static class ApiKey {
        private String headerName = "X-API-Key";
        private List<Entry> keys = new ArrayList<>();

        public String getHeaderName() {
            return headerName;
        }

        public void setHeaderName(String headerName) {
            this.headerName = headerName;
        }

        public List<Entry> getKeys() {
            return keys;
        }

        public void setKeys(List<Entry> keys) {
            this.keys = keys;
        }

        /** A single API key -> principal/roles mapping. */
        public static class Entry {
            private String key;
            private String principal;
            private String merchantId;
            private List<String> roles = new ArrayList<>();

            public String getKey() {
                return key;
            }

            public void setKey(String key) {
                this.key = key;
            }

            public String getPrincipal() {
                return principal;
            }

            public void setPrincipal(String principal) {
                this.principal = principal;
            }

            public String getMerchantId() {
                return merchantId;
            }

            public void setMerchantId(String merchantId) {
                this.merchantId = merchantId;
            }

            public List<String> getRoles() {
                return roles;
            }

            public void setRoles(List<String> roles) {
                this.roles = roles;
            }
        }
    }
}
