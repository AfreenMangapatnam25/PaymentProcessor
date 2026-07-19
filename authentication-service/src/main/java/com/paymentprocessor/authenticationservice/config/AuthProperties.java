package com.paymentprocessor.authenticationservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;

/**
 * Strongly-typed binding for all {@code auth.*} configuration.
 */
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

    private final Jwt jwt = new Jwt();
    private final Refresh refresh = new Refresh();
    private final Lockout lockout = new Lockout();
    private final RateLimit ratelimit = new RateLimit();
    private final Password password = new Password();
    private final Mfa mfa = new Mfa();
    private final Verification verification = new Verification();
    private final Reset reset = new Reset();
    private final Events events = new Events();
    private final Outbox outbox = new Outbox();

    public Jwt getJwt() { return jwt; }
    public Refresh getRefresh() { return refresh; }
    public Lockout getLockout() { return lockout; }
    public RateLimit getRatelimit() { return ratelimit; }
    public Password getPassword() { return password; }
    public Mfa getMfa() { return mfa; }
    public Verification getVerification() { return verification; }
    public Reset getReset() { return reset; }
    public Events getEvents() { return events; }
    public Outbox getOutbox() { return outbox; }

    public static class Jwt {
        private String issuer;
        private String audience;
        private Duration accessTokenTtl = Duration.ofMinutes(15);
        private String privateKeyPem = "";
        private String publicKeyPem = "";
        private String keyId = "";
        public String getIssuer() { return issuer; }
        public void setIssuer(String issuer) { this.issuer = issuer; }
        public String getAudience() { return audience; }
        public void setAudience(String audience) { this.audience = audience; }
        public Duration getAccessTokenTtl() { return accessTokenTtl; }
        public void setAccessTokenTtl(Duration accessTokenTtl) { this.accessTokenTtl = accessTokenTtl; }
        public String getPrivateKeyPem() { return privateKeyPem; }
        public void setPrivateKeyPem(String privateKeyPem) { this.privateKeyPem = privateKeyPem; }
        public String getPublicKeyPem() { return publicKeyPem; }
        public void setPublicKeyPem(String publicKeyPem) { this.publicKeyPem = publicKeyPem; }
        public String getKeyId() { return keyId; }
        public void setKeyId(String keyId) { this.keyId = keyId; }
    }

    public static class Refresh {
        private Duration ttl = Duration.ofDays(30);
        public Duration getTtl() { return ttl; }
        public void setTtl(Duration ttl) { this.ttl = ttl; }
    }

    public static class Lockout {
        private int maxFailedAttempts = 5;
        private Duration window = Duration.ofMinutes(15);
        private Duration lockDuration = Duration.ofMinutes(15);
        public int getMaxFailedAttempts() { return maxFailedAttempts; }
        public void setMaxFailedAttempts(int maxFailedAttempts) { this.maxFailedAttempts = maxFailedAttempts; }
        public Duration getWindow() { return window; }
        public void setWindow(Duration window) { this.window = window; }
        public Duration getLockDuration() { return lockDuration; }
        public void setLockDuration(Duration lockDuration) { this.lockDuration = lockDuration; }
    }

    public static class RateLimit {
        private int loginAttemptsPerMinute = 10;
        private int resetRequestsPerHour = 5;
        public int getLoginAttemptsPerMinute() { return loginAttemptsPerMinute; }
        public void setLoginAttemptsPerMinute(int v) { this.loginAttemptsPerMinute = v; }
        public int getResetRequestsPerHour() { return resetRequestsPerHour; }
        public void setResetRequestsPerHour(int v) { this.resetRequestsPerHour = v; }
    }

    public static class Password {
        private int minLength = 12;
        private boolean requireUppercase = true;
        private boolean requireLowercase = true;
        private boolean requireDigit = true;
        private boolean requireSpecial = true;
        private int historyDepth = 5;
        private Duration maxAge = Duration.ofDays(180);
        public int getMinLength() { return minLength; }
        public void setMinLength(int minLength) { this.minLength = minLength; }
        public boolean isRequireUppercase() { return requireUppercase; }
        public void setRequireUppercase(boolean v) { this.requireUppercase = v; }
        public boolean isRequireLowercase() { return requireLowercase; }
        public void setRequireLowercase(boolean v) { this.requireLowercase = v; }
        public boolean isRequireDigit() { return requireDigit; }
        public void setRequireDigit(boolean v) { this.requireDigit = v; }
        public boolean isRequireSpecial() { return requireSpecial; }
        public void setRequireSpecial(boolean v) { this.requireSpecial = v; }
        public int getHistoryDepth() { return historyDepth; }
        public void setHistoryDepth(int historyDepth) { this.historyDepth = historyDepth; }
        public Duration getMaxAge() { return maxAge; }
        public void setMaxAge(Duration maxAge) { this.maxAge = maxAge; }
    }

    public static class Mfa {
        private String issuerLabel = "PaymentProcessor";
        private int totpStepSeconds = 30;
        private int totpWindow = 1;
        private int recoveryCodeCount = 10;
        public String getIssuerLabel() { return issuerLabel; }
        public void setIssuerLabel(String issuerLabel) { this.issuerLabel = issuerLabel; }
        public int getTotpStepSeconds() { return totpStepSeconds; }
        public void setTotpStepSeconds(int totpStepSeconds) { this.totpStepSeconds = totpStepSeconds; }
        public int getTotpWindow() { return totpWindow; }
        public void setTotpWindow(int totpWindow) { this.totpWindow = totpWindow; }
        public int getRecoveryCodeCount() { return recoveryCodeCount; }
        public void setRecoveryCodeCount(int recoveryCodeCount) { this.recoveryCodeCount = recoveryCodeCount; }
    }

    public static class Verification {
        private Duration tokenTtl = Duration.ofMinutes(30);
        public Duration getTokenTtl() { return tokenTtl; }
        public void setTokenTtl(Duration tokenTtl) { this.tokenTtl = tokenTtl; }
    }

    public static class Reset {
        private Duration tokenTtl = Duration.ofMinutes(30);
        public Duration getTokenTtl() { return tokenTtl; }
        public void setTokenTtl(Duration tokenTtl) { this.tokenTtl = tokenTtl; }
    }

    public static class Outbox {
        private boolean relayEnabled = true;
        private int batchSize = 100;
        private int maxAttempts = 10;
        private long pollIntervalMs = 2000;
        public boolean isRelayEnabled() { return relayEnabled; }
        public void setRelayEnabled(boolean relayEnabled) { this.relayEnabled = relayEnabled; }
        public int getBatchSize() { return batchSize; }
        public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
        public long getPollIntervalMs() { return pollIntervalMs; }
        public void setPollIntervalMs(long pollIntervalMs) { this.pollIntervalMs = pollIntervalMs; }
    }

    public static class Events {
        private final Topics topics = new Topics();
        public Topics getTopics() { return topics; }
        public static class Topics {
            private String userLoggedIn;
            private String userLoggedOut;
            private String passwordChanged;
            private String mfaEnabled;
            private String accountLocked;
            private String newDevice;
            public String getUserLoggedIn() { return userLoggedIn; }
            public void setUserLoggedIn(String v) { this.userLoggedIn = v; }
            public String getUserLoggedOut() { return userLoggedOut; }
            public void setUserLoggedOut(String v) { this.userLoggedOut = v; }
            public String getPasswordChanged() { return passwordChanged; }
            public void setPasswordChanged(String v) { this.passwordChanged = v; }
            public String getMfaEnabled() { return mfaEnabled; }
            public void setMfaEnabled(String v) { this.mfaEnabled = v; }
            public String getAccountLocked() { return accountLocked; }
            public void setAccountLocked(String v) { this.accountLocked = v; }
            public String getNewDevice() { return newDevice; }
            public void setNewDevice(String v) { this.newDevice = v; }
        }
    }
}
