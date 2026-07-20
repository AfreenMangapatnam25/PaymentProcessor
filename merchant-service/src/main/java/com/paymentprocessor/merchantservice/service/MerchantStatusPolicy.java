package com.paymentprocessor.merchantservice.service;

import com.paymentprocessor.merchantservice.common.enums.MerchantStatus;
import com.paymentprocessor.merchantservice.common.error.InvalidStatusTransitionException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Encapsulates the legal merchant lifecycle transitions. TERMINATED and BLACKLISTED are terminal.
 */
@Component
public class MerchantStatusPolicy {

    private final Map<MerchantStatus, Set<MerchantStatus>> allowed = new EnumMap<>(MerchantStatus.class);

    public MerchantStatusPolicy() {
        allowed.put(MerchantStatus.PENDING,
                EnumSet.of(MerchantStatus.UNDER_REVIEW, MerchantStatus.TERMINATED, MerchantStatus.BLACKLISTED));
        allowed.put(MerchantStatus.UNDER_REVIEW,
                EnumSet.of(MerchantStatus.ACTIVE, MerchantStatus.PENDING, MerchantStatus.RESTRICTED,
                        MerchantStatus.TERMINATED, MerchantStatus.BLACKLISTED));
        allowed.put(MerchantStatus.ACTIVE,
                EnumSet.of(MerchantStatus.SUSPENDED, MerchantStatus.RESTRICTED, MerchantStatus.TERMINATED));
        allowed.put(MerchantStatus.SUSPENDED,
                EnumSet.of(MerchantStatus.ACTIVE, MerchantStatus.RESTRICTED, MerchantStatus.TERMINATED,
                        MerchantStatus.BLACKLISTED));
        allowed.put(MerchantStatus.RESTRICTED,
                EnumSet.of(MerchantStatus.ACTIVE, MerchantStatus.SUSPENDED, MerchantStatus.TERMINATED));
        allowed.put(MerchantStatus.TERMINATED, EnumSet.noneOf(MerchantStatus.class));
        allowed.put(MerchantStatus.BLACKLISTED, EnumSet.noneOf(MerchantStatus.class));
    }

    public boolean canTransition(MerchantStatus from, MerchantStatus to) {
        return from != to && allowed.getOrDefault(from, EnumSet.noneOf(MerchantStatus.class)).contains(to);
    }

    public void assertTransition(MerchantStatus from, MerchantStatus to) {
        if (!canTransition(from, to)) {
            throw new InvalidStatusTransitionException(from, to);
        }
    }
}
