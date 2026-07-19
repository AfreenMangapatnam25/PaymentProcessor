package com.paymentprocessor.settlementservice.web.dto;

import com.paymentprocessor.settlementservice.enums.ScheduleType;

/**
 * Request to trigger a settlement run. If {@code merchantId} and {@code currency}
 * are supplied, only that merchant is settled; otherwise the full cycle runs.
 */
public record RunCycleRequest(
        ScheduleType scheduleType,
        String merchantId,
        String currency
) {
    public ScheduleType scheduleTypeOrDefault() {
        return scheduleType == null ? ScheduleType.DAILY : scheduleType;
    }
}
