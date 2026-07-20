package com.paymentprocessor.authorization.service.payment;

import com.paymentprocessor.authorization.config.GatewayProperties;
import com.paymentprocessor.authorization.domain.payment.AuthorizationRecord;
import com.paymentprocessor.authorization.dto.payment.ReversalRequest;
import com.paymentprocessor.authorization.event.AuthorizationEvent;
import com.paymentprocessor.authorization.event.EventPublisher;
import com.paymentprocessor.authorization.exception.InvalidStateException;
import com.paymentprocessor.authorization.gateway.ResilientGateway;
import com.paymentprocessor.authorization.gateway.model.GatewayResult;
import com.paymentprocessor.authorization.gateway.model.GatewayReversalCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Reverses (voids) authorization holds before capture, releasing the held funds.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReversalService {

    private final PaymentAuthorizationService authorizationService;
    private final AuthorizationStore store;
    private final ResilientGateway gateway;
    private final EventPublisher eventPublisher;
    private final GatewayProperties gatewayProperties;

    public AuthorizationRecord reverse(UUID authorizationId, ReversalRequest request) {
        AuthorizationRecord record = authorizationService.getById(authorizationId);

        if (!record.getStatus().isReversible()) {
            throw new InvalidStateException(
                    "Authorization " + authorizationId + " cannot be reversed in status " + record.getStatus());
        }

        String reason = request != null ? request.reason() : null;
        GatewayResult result = gateway.reverse(GatewayReversalCommand.builder()
                .gatewayAuthorizationId(record.getGatewayAuthorizationId())
                .reason(reason)
                .idempotencyKey(authorizationId + ":reverse")
                .build());

        AuthorizationRecord updated = store.applyGatewayResult(
                authorizationId, result, gatewayProperties.getProvider());
        eventPublisher.publishAuthorizationEvent(
                AuthorizationEvent.of(AuthorizationEvent.Types.REVERSED, updated));
        log.info("Reversed authorization {} (reason={})", authorizationId, reason);
        return updated;
    }
}
