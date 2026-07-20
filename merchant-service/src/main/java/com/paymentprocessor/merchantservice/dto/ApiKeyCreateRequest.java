package com.paymentprocessor.merchantservice.dto;

import com.paymentprocessor.merchantservice.common.enums.ApiKeyType;
import jakarta.validation.constraints.*;
import java.time.Instant;

public record ApiKeyCreateRequest(
        @NotNull ApiKeyType keyType,
        @Size(max = 120) String label,
        @Size(max = 1024) String ipAllowlist,
        Instant expiresAt
) {}
