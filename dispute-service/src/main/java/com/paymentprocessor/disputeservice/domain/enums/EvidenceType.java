package com.paymentprocessor.disputeservice.domain.enums;

import java.util.Set;

/**
 * Supported evidence document format groups, each with an upload size ceiling
 * (in bytes) and the set of accepted file extensions.
 */
public enum EvidenceType {

    IMAGE(10L * 1024 * 1024, Set.of("jpg", "jpeg", "png", "tiff")),
    PDF(25L * 1024 * 1024, Set.of("pdf")),
    TEXT(5L * 1024 * 1024, Set.of("txt", "csv")),
    AUDIO_VIDEO(50L * 1024 * 1024, Set.of("mp3", "mp4", "wav")),
    EMAIL_EXPORT(10L * 1024 * 1024, Set.of("eml", "msg"));

    private final long maxSizeBytes;
    private final Set<String> extensions;

    EvidenceType(long maxSizeBytes, Set<String> extensions) {
        this.maxSizeBytes = maxSizeBytes;
        this.extensions = extensions;
    }

    public long getMaxSizeBytes() {
        return maxSizeBytes;
    }

    public Set<String> getExtensions() {
        return extensions;
    }

    /**
     * Resolves the evidence type that accepts the given file extension.
     *
     * @return the matching type, or {@code null} if no type accepts it
     */
    public static EvidenceType forExtension(String extension) {
        if (extension == null) {
            return null;
        }
        String normalized = extension.toLowerCase().replace(".", "").trim();
        for (EvidenceType type : values()) {
            if (type.extensions.contains(normalized)) {
                return type;
            }
        }
        return null;
    }
}
