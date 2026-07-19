package com.paymentprocessor.authorization.service.payment;

import com.paymentprocessor.authorization.config.AuthorizationProperties;
import com.paymentprocessor.authorization.domain.enums.AuthorizationStatus;
import com.paymentprocessor.authorization.domain.payment.AuthorizationRecord;
import com.paymentprocessor.authorization.gateway.model.GatewayResult;
import com.paymentprocessor.authorization.repository.AuthorizationRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Transactional persistence boundary for {@link AuthorizationRecord}. Keeping these operations in a
 * dedicated bean ensures {@code @Transactional} is honoured (Spring AOP does not apply to
 * self-invoked methods) and keeps the orchestration services free of persistence concerns.
 */
@Service
@RequiredArgsConstructor
public class AuthorizationStore {

    private final AuthorizationRecordRepository repository;
    private final AuthorizationProperties properties;

    @Transactional
    public AuthorizationRecord save(AuthorizationRecord record) {
        return repository.save(record);
    }

    /**
     * Merge a gateway {@link GatewayResult} into the stored record and persist it. Handles partial
     * approval, timestamps and hold expiry.
     */
    @Transactional
    public AuthorizationRecord applyGatewayResult(UUID id, GatewayResult result, String provider) {
        AuthorizationRecord record = repository.getReferenceById(id);
        AuthorizationStatus status = result.getStatus();

        if (status == AuthorizationStatus.APPROVED
                && result.getApprovedAmount() != null
                && record.getRequestedAmount() != null
                && result.getApprovedAmount().compareTo(record.getRequestedAmount()) < 0) {
            status = AuthorizationStatus.PARTIALLY_APPROVED;
        }

        record.setStatus(status);
        record.setApprovedAmount(result.getApprovedAmount());
        if (result.getCapturedAmount() != null) {
            record.setCapturedAmount(result.getCapturedAmount());
        }
        record.setAuthorizationCode(result.getAuthorizationCode());
        record.setNetworkReferenceId(result.getNetworkReferenceId());
        record.setGatewayProvider(provider);
        record.setGatewayAuthorizationId(result.getGatewayAuthorizationId());
        record.setGatewayResponseCode(result.getResponseCode());
        record.setGatewayResponseMessage(truncate(result.getResponseMessage()));
        record.setResponseCode(result.getResponseCode());
        record.setAvsResult(result.getAvsResult());
        record.setCvvResult(result.getCvvResult());
        record.setRequiresAuthentication(result.isRequiresAuthentication());
        record.setAuthenticationUrl(result.getAuthenticationUrl());
        if (result.getCardNetwork() != null) {
            record.setCardNetwork(result.getCardNetwork());
        }
        if (result.getCardLast4() != null) {
            record.setCardLast4(result.getCardLast4());
        }
        record.setGatewayRawResponse(result.getRawResponse());

        Instant now = Instant.now();
        if (status.isApproved()) {
            if (record.getAuthorizedAt() == null) {
                record.setAuthorizedAt(now);
            }
            if (record.getExpiresAt() == null) {
                record.setExpiresAt(now.plus(properties.getDefaultHoldMinutes(), ChronoUnit.MINUTES));
            }
        }
        if (status == AuthorizationStatus.CAPTURED || status == AuthorizationStatus.PARTIALLY_CAPTURED) {
            record.setCapturedAt(now);
        }
        if (status == AuthorizationStatus.REVERSED) {
            record.setReversedAt(now);
        }
        return repository.save(record);
    }

    @Transactional
    public void markFailed(UUID id, String message) {
        AuthorizationRecord record = repository.getReferenceById(id);
        record.setStatus(AuthorizationStatus.FAILED);
        record.setGatewayResponseMessage(truncate(message));
        repository.save(record);
    }

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > 500 ? value.substring(0, 500) : value;
    }
}
