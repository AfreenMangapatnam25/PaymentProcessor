package com.paymentprocessor.authorization.service.payment;

import com.paymentprocessor.authorization.config.GatewayProperties;
import com.paymentprocessor.authorization.domain.payment.AuthorizationRecord;
import com.paymentprocessor.authorization.dto.payment.CaptureRequest;
import com.paymentprocessor.authorization.event.AuthorizationEvent;
import com.paymentprocessor.authorization.event.EventPublisher;
import com.paymentprocessor.authorization.exception.InvalidStateException;
import com.paymentprocessor.authorization.exception.ValidationException;
import com.paymentprocessor.authorization.gateway.ResilientGateway;
import com.paymentprocessor.authorization.gateway.model.GatewayCaptureCommand;
import com.paymentprocessor.authorization.gateway.model.GatewayResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Captures previously authorized holds (full or partial), enforcing capture eligibility and
 * hold expiry.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptureService {

    private final PaymentAuthorizationService authorizationService;
    private final AuthorizationStore store;
    private final ResilientGateway gateway;
    private final EventPublisher eventPublisher;
    private final GatewayProperties gatewayProperties;

    public AuthorizationRecord capture(UUID authorizationId, CaptureRequest request) {
        AuthorizationRecord record = authorizationService.getById(authorizationId);

        if (!record.getStatus().isCapturable()) {
            throw new InvalidStateException(
                    "Authorization " + authorizationId + " is not capturable in status " + record.getStatus());
        }
        if (record.getExpiresAt() != null && record.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidStateException("Authorization hold has expired and cannot be captured");
        }

        BigDecimal alreadyCaptured = record.getCapturedAmount() == null ? BigDecimal.ZERO : record.getCapturedAmount();
        BigDecimal remaining = record.getApprovedAmount().subtract(alreadyCaptured);
        BigDecimal amount = request != null && request.amount() != null ? request.amount() : remaining;

        if (amount.signum() <= 0) {
            throw new ValidationException("Capture amount must be positive");
        }
        if (amount.compareTo(remaining) > 0) {
            throw new ValidationException("Capture amount " + amount
                    + " exceeds remaining authorized amount " + remaining);
        }

        GatewayResult result = gateway.capture(GatewayCaptureCommand.builder()
                .gatewayAuthorizationId(record.getGatewayAuthorizationId())
                .amount(amount)
                .currency(record.getCurrency())
                .idempotencyKey(authorizationId + ":capture:" + amount.toPlainString())
                .build());

        AuthorizationRecord updated = store.applyGatewayResult(
                authorizationId, result, gatewayProperties.getProvider());
        eventPublisher.publishAuthorizationEvent(
                AuthorizationEvent.of(AuthorizationEvent.Types.CAPTURED, updated));
        log.info("Captured {} {} on authorization {}", amount, record.getCurrency(), authorizationId);
        return updated;
    }
}
