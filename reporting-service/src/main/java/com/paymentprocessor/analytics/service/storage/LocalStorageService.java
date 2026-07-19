package com.paymentprocessor.analytics.service.storage;

import com.paymentprocessor.analytics.config.AnalyticsProperties;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** Filesystem-backed storage for local/dev. Not durable across instances. */
@Service
@ConditionalOnProperty(name = "analytics.storage.backend", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    private final Path root;

    public LocalStorageService(AnalyticsProperties props) throws IOException {
        this.root = Path.of(props.getStorage().getLocalDir());
        Files.createDirectories(root);
    }

    @Override
    public String backend() { return "local"; }

    @Override
    public StoredObject store(String key, String contentType, StreamWriter writer) throws IOException {
        Path target = resolve(key);
        Files.createDirectories(target.getParent());
        try (OutputStream os = new BufferedOutputStream(Files.newOutputStream(target))) {
            writer.writeTo(os);
        }
        return new StoredObject(key, Files.size(target));
    }

    @Override
    public InputStream open(String key) throws IOException {
        return Files.newInputStream(resolve(key));
    }

    @Override
    public Optional<String> presignedUrl(String key, String filename, String contentType) {
        return Optional.empty(); // served via the API download endpoint instead
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to delete " + key, e);
        }
    }

    @Override
    public boolean exists(String key) {
        return Files.exists(resolve(key));
    }

    private Path resolve(String key) {
        Path p = root.resolve(key).normalize();
        if (!p.startsWith(root)) {
            throw new IllegalArgumentException("Path traversal blocked for key: " + key);
        }
        return p;
    }
}
