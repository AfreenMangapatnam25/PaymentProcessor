package com.paymentprocessor.limit.dto;

import com.paymentprocessor.limit.domain.enums.EnforcementMode;
import com.paymentprocessor.limit.domain.enums.EntityScope;
import com.paymentprocessor.limit.domain.enums.LimitDimension;
import com.paymentprocessor.limit.domain.enums.TimeWindow;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Create/replace payload for a limit configuration.
 */
public record LimitConfigRequest(

        @NotBlank
        @Size(max = 200)
        String name,

        @NotNull
        EntityScope scope,

        /** Null => a scope-wide default applying to every entity in the scope. */
        @Size(max = 100)
        String scopeId,

        @NotNull
        LimitDimension dimension,

        @NotNull
        TimeWindow timeWindow,

        @NotNull
        @DecimalMin(value = "0.0001", message = "threshold must be positive")
        BigDecimal threshold,

        @Size(min = 3, max = 3)
        String currency,

        @NotNull
        EnforcementMode enforcement,

        Integer priority,

        Boolean active,

        @Size(max = 60)
        String timeZone
) {}
