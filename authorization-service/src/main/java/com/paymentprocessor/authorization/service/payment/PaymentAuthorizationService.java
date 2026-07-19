package com.paymentprocessor.authorization.service.payment;

import com.paymentprocessor.authorization.config.AuthorizationProperties;
import com.paymentprocessor.authorization.config.GatewayProperties;
import com.paymentprocessor.authorization.domain.enums.AuthorizationStatus;
import com.paymentprocessor.authorization.domain.enums.AuthorizationType;
import com.paymentprocessor.authorization.domain.enums.CardNetwork;
import com.paymentprocessor.authorization.domain.payment.AuthorizationRecord;
import com.paymentprocessor.authorization.dto.payment.AuthorizationRequest;
import com.paymentprocessor.authorization.event.AuthorizationEvent;
import com.paymentprocessor.authorization.event.EventPublisher;
import com.paymentprocessor.authorization.exception.ResourceNotFoundException;
import com.paymentprocessor.authorization.exception.ValidationException;
import com.paymentprocessor.authorization.gateway.ResilientGateway;
import com.paymentprocessor.authorization.gateway.model.GatewayAuthorizeCommand;
import com.paymentprocessor.authorization.gateway.model.GatewayResult;
import com.paymentprocessor.authorization.repository.AuthorizationRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestrates payment authorization: idempotency and duplicate handling, gateway communication,
 * metadata persistence, and event publication.
 *
 * <p>The external gateway call is deliberately performed outside any database transaction to avoid
 * holding a connection open across a network round-trip. Persistence is delegated to
 * {@link AuthorizationStore}, whose methods are individually transactional.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentAuthorizationService {

    private static final List<AuthorizationStatus> ACTIVE_STATUSES = List.of(
            AuthorizationStatus.PENDING,
            AuthorizationStatus.APPROVED,
            AuthorizationStatus.PARTIALLY_APPROVED,
            AuthorizationStatus.REQUIRES_AUTHENTICATION);

    private final AuthorizationRecordRepository repository;
    private final AuthorizationStore store;
    private final ResilientGateway gateway;
    private final IdempotencyService idempotencyService;
    private final EventPublisher eventPublisher;
    private final AuthorizationProperties properties;
    private final GatewayProperties gatewayProperties;

    public AuthorizationRecord authorize(AuthorizationRequest request, String idempotencyKey) {
        String requestHash = idempotencyService.hash(request);

        // 1. Idempotent replay.
        Optional<UUID> replay = idempotencyService.findExisting(idempotencyKey, requestHash);
        if (replay.isPresent()) {
            log.info("Idempotent replay for key {} -> authorization {}", idempotencyKey, replay.get());
            return getById(replay.get());
        }

        // 2. Duplicate active authorization for the same payment.
        Optional<AuthorizationRecord> duplicate = repository.findByPaymentReference(request.paymentReference())
                .stream()
                .filter(a -> ACTIVE_STATUSES.contains(a.getStatus()))
                .findFirst();
        if (duplicate.isPresent()) {
            log.info("Duplicate authorization request for paymentReference {}; returning existing {}",
                    request.paymentReference(), duplicate.get().getId());
            return duplicate.get();
        }

        // 3. Persist a PENDING record before contacting the gateway.
        AuthorizationRecord pending = store.save(buildPending(request, idempotencyKey));

        // 4. Contact the gateway (outside a transaction).
        GatewayResult result;
        try {
            result = gateway.authorize(toCommand(request, pending));
        } catch (RuntimeException ex) {
            store.markFailed(pending.getId(), ex.getMessage());
            throw ex;
        }

        // 5. Apply the result and persist.
        AuthorizationRecord finalized = store.applyGatewayResult(
                pending.getId(), result, gatewayProperties.getProvider());

        // 6. Record idempotency mapping and publish the outcome.
        idempotencyService.record(idempotencyKey, requestHash, "authorization", finalized.getId());
        publishOutcome(finalized);
        return finalized;
    }

    @Transactional(readOnly = true)
    public AuthorizationRecord getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Authorization not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<AuthorizationRecord> getByPaymentReference(String paymentReference) {
        List<AuthorizationRecord> records = repository.findByPaymentReference(paymentReference);
        if (records.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No authorizations found for paymentReference: " + paymentReference);
        }
        return records;
    }

    /**
     * Refresh a pending / authentication-required authorization from the gateway (e.g. after a
     * 3-D Secure challenge completes).
     */
    public AuthorizationRecord synchronize(UUID id) {
        AuthorizationRecord record = getById(id);
        if (record.getGatewayAuthorizationId() == null) {
            return record;
        }
        AuthorizationStatus previous = record.getStatus();
        GatewayResult result = gateway.retrieve(record.getGatewayAuthorizationId());
        AuthorizationRecord updated = store.applyGatewayResult(
                id, result, gatewayProperties.getProvider());
        if (updated.getStatus() != previous) {
            publishOutcome(updated);
        }
        return updated;
    }

    // ------------------------------------------------------------------ helpers

    private AuthorizationRecord buildPending(AuthorizationRequest request, String idempotencyKey) {
        CardNetwork network = request.cardBin() != null ? CardNetwork.fromPan(request.cardBin()) : null;
        return AuthorizationRecord.builder()
                .id(UUID.randomUUID())
                .status(AuthorizationStatus.PENDING)
                .type(AuthorizationType.INITIAL)
                .merchantId(request.merchantId())
                .customerId(request.customerId())
                .paymentReference(request.paymentReference())
                .requestedAmount(request.amount())
                .currency(request.currency().toUpperCase())
                .cardNetwork(network)
                .cardBin(request.cardBin())
                .cardLast4(request.cardLast4())
                .cardExpMonth(request.cardExpMonth())
                .cardExpYear(request.cardExpYear())
                .idempotencyKey(idempotencyKey)
                .riskScore(request.riskScore())
                .reauthorizationCount(0)
                .requiresAuthentication(false)
                .build();
    }

    private GatewayAuthorizeCommand toCommand(AuthorizationRequest request, AuthorizationRecord record) {
        if (request.amount().signum() <= 0) {
            throw new ValidationException("Authorization amount must be positive");
        }
        return GatewayAuthorizeCommand.builder()
                .merchantId(request.merchantId())
                .paymentReference(request.paymentReference())
                .amount(request.amount())
                .currency(request.currency().toUpperCase())
                .paymentMethodToken(request.paymentMethodToken())
                .customerReference(request.customerId())
                .statementDescriptor(request.statementDescriptor())
                .captureImmediately(request.captureImmediately())
                .threeDsEnabled(properties.isThreeDsEnabled())
                .avsRequired(properties.isAvsRequired())
                .idempotencyKey(record.getId().toString())
                .metadata(request.metadata() == null ? Map.of() : request.metadata())
                .build();
    }

    private void publishOutcome(AuthorizationRecord record) {
        String type = switch (record.getStatus()) {
            case APPROVED -> AuthorizationEvent.Types.AUTHORIZED;
            case PARTIALLY_APPROVED -> AuthorizationEvent.Types.PARTIALLY_APPROVED;
            case REQUIRES_AUTHENTICATION -> AuthorizationEvent.Types.AUTHENTICATION_REQUIRED;
            case DECLINED, FAILED -> AuthorizationEvent.Types.DECLINED;
            case CAPTURED -> AuthorizationEvent.Types.CAPTURED;
            case REVERSED -> AuthorizationEvent.Types.REVERSED;
            default -> null;
        };
        if (type != null) {
            eventPublisher.publishAuthorizationEvent(AuthorizationEvent.of(type, record));
        }
    }
}
