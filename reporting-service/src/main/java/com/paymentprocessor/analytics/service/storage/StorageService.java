package com.paymentprocessor.analytics.service.storage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/** Abstraction over report-file persistence. Implemented by local disk and S3. */
public interface StorageService {

    String backend();

    /** Persist the streamed content under {@code key}; returns the key and byte size. */
    StoredObject store(String key, String contentType, StreamWriter writer) throws IOException;

    /** Open a stored object for download (used by the local download endpoint / S3 proxy). */
    InputStream open(String key) throws IOException;

    /** A time-limited direct download URL, when the backend supports it (S3 presigned). */
    Optional<String> presignedUrl(String key, String filename, String contentType);

    void delete(String key);

    boolean exists(String key);
}
