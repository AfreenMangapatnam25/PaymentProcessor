package com.paymentprocessor.settlementservice.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strongly-typed configuration for the settlement domain, bound from the
 * {@code settlement.*} namespace in application.yml.
 */
@ConfigurationProperties(prefix = "settlement")
public class SettlementProperties {

    /** Minimum net payout (minor units) before a batch is released. */
    private long minimumPayoutMinor = 2500;

    /** Rolling reserve default hold period, in days. */
    private int reserveHoldDays = 90;

    private Retry retry = new Retry();
    private Approval approval = new Approval();
    private Scheduler scheduler = new Scheduler();

    public long getMinimumPayoutMinor() { return minimumPayoutMinor; }
    public void setMinimumPayoutMinor(long v) { this.minimumPayoutMinor = v; }

    public int getReserveHoldDays() { return reserveHoldDays; }
    public void setReserveHoldDays(int v) { this.reserveHoldDays = v; }

    public Retry getRetry() { return retry; }
    public void setRetry(Retry retry) { this.retry = retry; }

    public Approval getApproval() { return approval; }
    public void setApproval(Approval approval) { this.approval = approval; }

    public Scheduler getScheduler() { return scheduler; }
    public void setScheduler(Scheduler scheduler) { this.scheduler = scheduler; }

    public static class Retry {
        private int maxAttempts = 5;
        private List<Integer> backoffMinutes = List.of(60, 240, 720, 1440, 2880);

        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int v) { this.maxAttempts = v; }

        public List<Integer> getBackoffMinutes() { return backoffMinutes; }
        public void setBackoffMinutes(List<Integer> v) { this.backoffMinutes = v; }

        /** Backoff delay (minutes) for a given 1-based attempt number, capped at the last configured value. */
        public int backoffForAttempt(int attemptNumber) {
            if (backoffMinutes == null || backoffMinutes.isEmpty()) {
                return 60;
            }
            int idx = Math.max(0, Math.min(attemptNumber - 1, backoffMinutes.size() - 1));
            return backoffMinutes.get(idx);
        }
    }

    public static class Approval {
        private long supervisorThresholdMinor = 100000;
        private long financeDirectorThresholdMinor = 1000000;
        private long reversalFinanceDirectorThresholdMinor = 500000;

        public long getSupervisorThresholdMinor() { return supervisorThresholdMinor; }
        public void setSupervisorThresholdMinor(long v) { this.supervisorThresholdMinor = v; }

        public long getFinanceDirectorThresholdMinor() { return financeDirectorThresholdMinor; }
        public void setFinanceDirectorThresholdMinor(long v) { this.financeDirectorThresholdMinor = v; }

        public long getReversalFinanceDirectorThresholdMinor() { return reversalFinanceDirectorThresholdMinor; }
        public void setReversalFinanceDirectorThresholdMinor(long v) { this.reversalFinanceDirectorThresholdMinor = v; }
    }

    public static class Scheduler {
        private boolean enabled = true;
        private String cycleCron = "0 0 * * * *";
        private String retryCron = "0 */15 * * * *";
        private String reserveReleaseCron = "0 0 2 * * *";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean v) { this.enabled = v; }

        public String getCycleCron() { return cycleCron; }
        public void setCycleCron(String v) { this.cycleCron = v; }

        public String getRetryCron() { return retryCron; }
        public void setRetryCron(String v) { this.retryCron = v; }

        public String getReserveReleaseCron() { return reserveReleaseCron; }
        public void setReserveReleaseCron(String v) { this.reserveReleaseCron = v; }
    }
}
