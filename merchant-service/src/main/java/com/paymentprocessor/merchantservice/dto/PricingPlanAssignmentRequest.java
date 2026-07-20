package com.paymentprocessor.merchantservice.dto;

import com.paymentprocessor.merchantservice.common.enums.PricingPlan;
import jakarta.validation.constraints.NotNull;

public record PricingPlanAssignmentRequest(@NotNull PricingPlan pricingPlan) {}
