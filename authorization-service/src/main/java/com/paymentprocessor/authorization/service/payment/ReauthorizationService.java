package com.paymentprocessor.authorization.service.payment;

import com.paymentprocessor.authorization.config.AuthorizationProperties;
import com.paymentprocessor.authorization.config.GatewayProperties;
import com.paymentprocessor.authorization.domain.enums.AuthorizationStatus;
import com.paymentprocessor.authorization.domain.enums.AuthorizationType;
import com.paymentprocessor.authorization.domain.payment.AuthorizationRecord;
import com.paymentprocessor.authorization.dto.payment.ReauthorizationRequest;
import com.paymentprocessor.authorization.event.AuthorizationEvent;
import com.paymentprocessor.authorization.event.EventPublisher;
import com.paymentprocessor.authorization.exception.InvalidStateException;
import com.paymentprocessor.authorization.exception.ValidationException;
import com.paymentprocessor.authorization.gateway.ResilientGateway;
import com.paymentprocessor.authorization.gateway.model.GatewayAuthorizeCommand;
import com.paymentprocessor.authorization.gateway.model.GatewayResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Re-authorizes a payment whose original authorization has expired or was insufficient, creating a
 * new authorization linked to the original.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReauthorizationService {

    private static final Set<AuthorizationStatus> REAUTHORIZABLE = EnumSet.of(
            AuthorizationStatus.EXPIRED,
            AuthorizationStatus.APPROVED,
            AuthorizationStatus.PARTIALLY_APPROVED,
            AuthorizationStatus.REVERSED);

    private final PaymentAuthorizationService authorizationService;
    private final AuthorizationStore store;
    private final ResilientGateway gateway;
    private final EventPublisher eventPublisher;
    private final AuthorizationProperties properties;
    private final GatewayProperties gatewayProperties;

    public AuthorizationRecord reauthorize(UUID originalId, ReauthorizationRequest request) {
        AuthorizationRecord original = authorizationService.getById(originalId);

        if (!REAUTHORIZABLE.contains(original.getStatus())) {
            throw new InvalidStateException(
                    "Authorization " + originalId + " cannot be re-authorized in status " + original.getStatus());
        }
        if (original.getReauthorizationCount() >= properties.getMaxReauthorizations()) {
            throw new ValidationException("Maximum re-authorizations ("
                    + properties.getMaxReauthorizations() + ") reached for authorization " + originalId);
        }

        BigDecimal amount = request.amount() != null ? request.amount() : original.getRequestedAmount();

        AuthorizationRecord pending = store.save(AuthorizationRecord.builder()
                .id(UUID.randomUUID())
                .status(AuthorizationStatus.PENDING)
                .type(AuthorizationType.REAUTHORIZATION)
                .merchantId(original.getMerchantId())
                .customerId(original.getCustomerId())
                .paymentReference(original.getPaymentReference())
                .requestedAmount(amount)
                .currency(original.getCurrency())
                .originalAuthorizationId(original.getId())
                .reauthorizationCount(original.getReauthorizationCount() + 1)
                .requiresAuthentication(false)
                .build());

        GatewayResult result;
        try {
            result = gateway.authorize(GatewayAuthorizeCommand.builder()
                    .merchantId(original.getMerchantId())
                    .paymentReference(original.getPaymentReference())
                    .amount(amount)
                    .currency(original.getCurrency())
                    .paymentMethodToken(request.paymentMethodToken())
                    .customerReference(original.getCustomerId())
                    .captureImmediately(false)
                    .threeDsEnabled(properties.isThreeDsEnabled())
                    .avsRequired(properties.isAvsRequired())
                    .idempotencyKey(pending.getId().toString())
                    .metadata(Map.of("reauthorization_of", original.getId().toString()))
                    .build());
        } catch (RuntimeException ex) {
            store.markFailed(pending.getId(), ex.getMessage());
            throw ex;
        }

        AuthorizationRecord finalized = store.applyGatewayResult(
                pending.getId(), result, gatewayProperties.getProvider());
        eventPublisher.publishAuthorizationEvent(
                AuthorizationEvent.of(AuthorizationEvent.Types.REAUTHORIZED, finalized));
        log.info("Re-authorized {} as new authorization {} (original {})",
                amount, finalized.getId(), originalId);
        return finalized;
    }
}
