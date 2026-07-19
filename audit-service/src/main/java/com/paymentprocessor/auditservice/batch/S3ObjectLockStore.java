package com.paymentprocessor.auditservice.batch;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.paymentprocessor.auditservice.config.AuditProperties;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ObjectLockMode;
import software.amazon.awssdk.services.s3.model.ObjectLockRetentionMode;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

/**
 * Writes the legal copy of a daily batch to S3 with Object Lock in COMPLIANCE mode.
 *
 * <p>COMPLIANCE retention means the object cannot be overwritten or deleted — not even
 * by the root account — until the retain-until date passes. Combined with a versioned,
 * Object-Lock-enabled bucket, this gives true WORM (write once, read many) storage for
 * the retention period (7–10 years).
 */
@Component
@ConditionalOnProperty(prefix = "audit.s3", name = "enabled", havingValue = "true")
public class S3ObjectLockStore {

    private static final Logger log = LoggerFactory.getLogger(S3ObjectLockStore.class);

    private final S3Client s3;
    private final AuditProperties.S3 config;

    public S3ObjectLockStore(S3Client s3, AuditProperties props) {
        this.s3 = s3;
        this.config = props.getS3();
    }

    public record StoredObject(String bucket, String key, String versionId, Instant retainUntil) {
    }

    /**
     * Puts an immutable object under Object Lock.
     *
     * @param key         object key
     * @param body        object bytes (the batch content)
     * @param retainUntil Object Lock retain-until instant
     * @param contentType MIME type
     */
    public StoredObject putImmutable(String key, byte[] body, Instant retainUntil, String contentType) {
        ObjectLockMode mode = "GOVERNANCE".equalsIgnoreCase(config.getObjectLockMode())
                ? ObjectLockMode.GOVERNANCE
                : ObjectLockMode.COMPLIANCE;

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(config.getBucket())
                .key(key)
                .contentType(contentType)
                .contentMD5(md5Base64(body))
                .objectLockMode(mode)
                .objectLockRetainUntilDate(retainUntil)
                .objectLockLegalHoldStatus(software.amazon.awssdk.services.s3.model
                        .ObjectLockLegalHoldStatus.OFF)
                .build();

        PutObjectResponse response = s3.putObject(request, RequestBody.fromBytes(body));
        log.info("Stored batch object s3://{}/{} versionId={} retainUntil={} mode={}",
                config.getBucket(), key, response.versionId(), retainUntil, mode);
        return new StoredObject(config.getBucket(), key, response.versionId(), retainUntil);
    }

    /** Retention mode as configured, for logging/manifest purposes. */
    public ObjectLockRetentionMode retentionMode() {
        return "GOVERNANCE".equalsIgnoreCase(config.getObjectLockMode())
                ? ObjectLockRetentionMode.GOVERNANCE
                : ObjectLockRetentionMode.COMPLIANCE;
    }

    private String md5Base64(byte[] body) {
        try {
            byte[] digest = MessageDigest.getInstance("MD5").digest(body);
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute content MD5", e);
        }
    }
}
