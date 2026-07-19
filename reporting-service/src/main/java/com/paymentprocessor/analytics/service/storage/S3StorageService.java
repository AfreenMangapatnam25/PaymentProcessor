package com.paymentprocessor.analytics.service.storage;

import com.paymentprocessor.analytics.config.AnalyticsProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

/** S3 (or S3-compatible, e.g. MinIO) object storage with presigned download URLs. */
@Service
@ConditionalOnProperty(name = "analytics.storage.backend", havingValue = "s3")
public class S3StorageService implements StorageService {

    private final S3Client s3;
    private final S3Presigner presigner;
    private final String bucket;
    private final Duration presignTtl;

    public S3StorageService(S3Client s3, S3Presigner presigner, AnalyticsProperties props) {
        this.s3 = s3;
        this.presigner = presigner;
        this.bucket = props.getStorage().getS3().getBucket();
        this.presignTtl = Duration.ofMinutes(props.getStorage().getS3().getPresignTtlMinutes());
    }

    @Override
    public String backend() { return "s3"; }

    @Override
    public StoredObject store(String key, String contentType, StreamWriter writer) throws IOException {
        // Stream to a temp file first so we can supply a content length to S3.
        Path tmp = Files.createTempFile("report-", ".tmp");
        try {
            try (var os = Files.newOutputStream(tmp)) {
                writer.writeTo(os);
            }
            long size = Files.size(tmp);
            s3.putObject(PutObjectRequest.builder()
                            .bucket(bucket).key(key).contentType(contentType).contentLength(size).build(),
                    RequestBody.fromFile(tmp));
            return new StoredObject(key, size);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Override
    public InputStream open(String key) {
        return s3.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build());
    }

    @Override
    public Optional<String> presignedUrl(String key, String filename, String contentType) {
        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(bucket).key(key)
                .responseContentDisposition("attachment; filename=\"" + filename + "\"")
                .responseContentType(contentType)
                .build();
        var presigned = presigner.presignGetObject(GetObjectPresignRequest.builder()
                .signatureDuration(presignTtl).getObjectRequest(get).build());
        return Optional.of(presigned.url().toString());
    }

    @Override
    public void delete(String key) {
        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }

    @Override
    public boolean exists(String key) {
        try {
            s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }
}
