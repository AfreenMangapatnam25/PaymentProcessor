package com.paymentprocessor.userservice.infrastructure.encryption;

import com.paymentprocessor.userservice.common.constants.ErrorCode;
import com.paymentprocessor.userservice.common.exception.InfrastructureException;
import com.paymentprocessor.userservice.common.id.IdGenerator;
import com.paymentprocessor.userservice.common.util.ClockProvider;
import com.paymentprocessor.userservice.infrastructure.config.AppProperties;
import com.paymentprocessor.userservice.infrastructure.encryption.kms.MasterKeyProvider;
import com.paymentprocessor.userservice.infrastructure.persistence.entity.CryptoKeyEntity;
import com.paymentprocessor.userservice.infrastructure.persistence.repository.CryptoKeyJpaRepository;
import org.springframework.stereotype.Service;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.util.Optional;

/**
 * Lifecycle manager for per-subject Data Encryption Keys (envelope encryption).
 *
 * <ul>
 *   <li><b>issue</b> — generate a random DEK, wrap it with the KMS master key,
 *       persist the wrapped bytes in {@code crypto_keys}, return the plaintext
 *       DEK for immediate use.</li>
 *   <li><b>load</b> — fetch the ACTIVE key, unwrap it. Empty if destroyed.</li>
 *   <li><b>destroy</b> — the crypto-shred: null the wrapped bytes, mark
 *       DESTROYED. After this the subject's ciphertext is unrecoverable.</li>
 * </ul>
 */
@Service
public class DataKeyService {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_DESTROYED = "DESTROYED";
    private static final String KEY_ID_PREFIX = "cky";

    private final CryptoKeyJpaRepository repository;
    private final MasterKeyProvider masterKeyProvider;
    private final IdGenerator idGenerator;
    private final ClockProvider clock;
    private final int keySizeBits;
    private final String algorithm;

    public DataKeyService(CryptoKeyJpaRepository repository,
                          MasterKeyProvider masterKeyProvider,
                          IdGenerator idGenerator,
                          ClockProvider clock,
                          AppProperties properties) {
        this.repository = repository;
        this.masterKeyProvider = masterKeyProvider;
        this.idGenerator = idGenerator;
        this.clock = clock;
        this.keySizeBits = properties.crypto().dataKey().keySizeBits();
        this.algorithm = properties.crypto().dataKey().algorithm();
    }

    /**
     * Issue a fresh DEK for a subject and persist its wrapped form. Runs in the
     * caller's transaction so the key row commits atomically with the entity.
     */
    public IssuedDataKey issueFor(String subjectType, String subjectId) {
        SecretKey dek = generateDek();
        byte[] wrapped = masterKeyProvider.wrap(dek);

        CryptoKeyEntity entity = new CryptoKeyEntity();
        entity.setId(idGenerator.generatePrefixedId(KEY_ID_PREFIX));
        entity.setSubjectType(subjectType);
        entity.setSubjectId(subjectId);
        entity.setWrappedDek(wrapped);
        entity.setKeyVersion(1);
        entity.setAlgorithm("AES_256_GCM");
        entity.setKmsMasterKeyId(masterKeyProvider.masterKeyId());
        entity.setStatus(STATUS_ACTIVE);
        entity.setCreatedAt(clock.now());
        repository.save(entity);

        return new IssuedDataKey(entity.getId(), dek);
    }

    /**
     * Load and unwrap the DEK for a crypto_key id. Empty if the key is missing
     * or has been destroyed (crypto-shredded).
     */
    public Optional<SecretKey> loadActiveKey(String cryptoKeyId) {
        if (cryptoKeyId == null) {
            return Optional.empty();
        }
        return repository.findByIdAndStatus(cryptoKeyId, STATUS_ACTIVE)
                .map(e -> masterKeyProvider.unwrap(e.getWrappedDek()));
    }

    /**
     * Load the ACTIVE DEK for a subject together with its key id. Used by
     * dependent aggregates (e.g. addresses) that encrypt with their owner's key
     * rather than a key of their own. Empty if the subject has no active key
     * (never created or already crypto-shredded).
     */
    public Optional<IssuedDataKey> loadActiveKeyForSubject(String subjectType, String subjectId) {
        return repository.findBySubjectTypeAndSubjectIdAndStatus(subjectType, subjectId, STATUS_ACTIVE)
                .map(e -> new IssuedDataKey(e.getId(), masterKeyProvider.unwrap(e.getWrappedDek())));
    }

    /**
     * Crypto-shred a subject: destroy the ACTIVE DEK. Idempotent -- returns
     * false if there was no active key to destroy.
     */
    public boolean destroyFor(String subjectType, String subjectId) {
        Optional<CryptoKeyEntity> active =
                repository.findBySubjectTypeAndSubjectIdAndStatus(subjectType, subjectId, STATUS_ACTIVE);
        if (active.isEmpty()) {
            return false;
        }
        CryptoKeyEntity entity = active.get();
        entity.setWrappedDek(null);
        entity.setStatus(STATUS_DESTROYED);
        entity.setDestroyedAt(clock.now());
        repository.save(entity);
        return true;
    }

    private SecretKey generateDek() {
        try {
            KeyGenerator generator = KeyGenerator.getInstance(algorithm);
            generator.init(keySizeBits);
            return generator.generateKey();
        } catch (Exception e) {
            throw new InfrastructureException(ErrorCode.ENCRYPTION_ERROR, "Failed to generate data key", e);
        }
    }
}
