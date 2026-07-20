package com.paymentprocessor.userservice.infrastructure.encryption;

import com.paymentprocessor.userservice.application.port.out.CryptoShredderPort;
import org.springframework.stereotype.Service;

/**
 * Infrastructure adapter implementing {@link CryptoShredderPort} by destroying
 * the subject's DEK via {@link DataKeyService}. Runs inside the caller's
 * transaction so the shred commits atomically with the domain state change.
 */
@Service
public class CryptoShreddingService implements CryptoShredderPort {

    private final DataKeyService dataKeyService;

    public CryptoShreddingService(DataKeyService dataKeyService) {
        this.dataKeyService = dataKeyService;
    }

    @Override
    public boolean shred(String subjectType, String subjectId) {
        return dataKeyService.destroyFor(subjectType, subjectId);
    }
}
