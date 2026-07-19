package com.paymentprocessor.userservice.common.exception;

import com.paymentprocessor.userservice.common.constants.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when infrastructure/downstream systems fail.
 * Used for: Kafka Down, Redis Down, Encryption Failed, Vault Failed
 */
public class InfrastructureException extends ApplicationException {

    private final String systemName;
    private final String operation;

    /**
     * Constructor with system name and operation.
     */
    public InfrastructureException(String systemName, String operation, Throwable cause) {
        super(
                mapErrorCode(systemName),
                String.format("Infrastructure error: %s failed during %s", systemName, operation),
                cause
        );
        this.systemName = systemName;
        this.operation = operation;
    }

    /**
     * Constructor with error code and custom message.
     */
    public InfrastructureException(ErrorCode errorCode, String message) {
        super(errorCode, HttpStatus.INTERNAL_SERVER_ERROR, message);
        this.systemName = null;
        this.operation = null;
    }

    /**
     * Constructor with error code, message, and cause.
     */
    public InfrastructureException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode,message, cause);
        this.systemName = null;
        this.operation = null;
    }

    /**
     * Constructor with system name only.
     */
    public InfrastructureException(String systemName) {
        super(
                mapErrorCode(systemName),
                HttpStatus.INTERNAL_SERVER_ERROR,
                String.format("%s is unavailable", systemName)
        );
        this.systemName = systemName;
        this.operation = null;
    }

    private static ErrorCode mapErrorCode(String systemName) {
        String lower = systemName.toLowerCase();
        if (lower.contains("kafka") || lower.contains("messaging")) {
            return ErrorCode.INTERNAL_SERVER_ERROR; // Could add KAFKA_DOWN to enum
        } else if (lower.contains("redis") || lower.contains("cache")) {
            return ErrorCode.INTERNAL_SERVER_ERROR; // Could add REDIS_DOWN to enum
        } else if (lower.contains("encrypt") || lower.contains("crypto")) {
            return ErrorCode.ENCRYPTION_ERROR;
        } else if (lower.contains("vault") || lower.contains("secret")) {
            return ErrorCode.INTERNAL_SERVER_ERROR; // Could add VAULT_FAILED to enum
        }
        return ErrorCode.INTERNAL_SERVER_ERROR;
    }

    // Static factory methods for common infrastructure failures

    /**
     * Creates a Kafka-specific infrastructure exception.
     */
    public static InfrastructureException kafkaDown(String topic, Throwable cause) {
        return new InfrastructureException(
                "Kafka",
                "publishing to topic " + topic,
                cause
        );
    }

    /**
     * Creates a Redis-specific infrastructure exception.
     */
    public static InfrastructureException redisDown(String operation, Throwable cause) {
        return new InfrastructureException(
                "Redis",
                operation,
                cause
        );
    }

    /**
     * Creates an encryption failure exception.
     */
    public static InfrastructureException encryptionFailed(String operation, Throwable cause) {
        return new InfrastructureException(
                ErrorCode.ENCRYPTION_ERROR,
                String.format("Encryption failed during %s", operation),
                cause
        );
    }

    /**
     * Creates a Vault-specific infrastructure exception.
     */
    public static InfrastructureException vaultFailed(String operation, Throwable cause) {
        return new InfrastructureException(
                "Vault",
                operation,
                cause
        );
    }

    // Getters
    public String getSystemName() {
        return systemName;
    }

    public String getOperation() {
        return operation;
    }
}