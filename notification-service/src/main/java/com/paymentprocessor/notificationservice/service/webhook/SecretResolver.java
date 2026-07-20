package com.paymentprocessor.notificationservice.service.webhook;

/**
 * Resolves webhook_endpoints.secret_ref (a KMS key reference) to the actual
 * signing secret. Swap the implementation for a real KMS client (AWS KMS,
 * GCP KMS, Vault) in production; see EnvSecretResolver for the local/dev
 * default.
 */
public interface SecretResolver {
    String resolve(String secretRef);
}
