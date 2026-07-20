package com.paymentprocessor.merchantservice.security;

import com.paymentprocessor.merchantservice.common.crypto.SecretHasher;
import com.paymentprocessor.merchantservice.common.enums.ApiKeyStatus;
import com.paymentprocessor.merchantservice.common.enums.ApiKeyType;
import com.paymentprocessor.merchantservice.entity.ApiKey;
import com.paymentprocessor.merchantservice.repository.ApiKeyRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Validates a presented credential and produces an {@link ApiKeyAuthenticationToken}.
 * Supports a static platform admin credential and DB-backed merchant API keys of the form
 * {@code <keyId>.<secret>}.
 */
@Service
public class ApiKeyAuthenticator {

    private final ApiKeyRepository apiKeyRepository;
    private final SecretHasher secretHasher;
    private final String adminApiKey;

    public ApiKeyAuthenticator(ApiKeyRepository apiKeyRepository, SecretHasher secretHasher,
                               @Value("${merchant-service.security.admin-api-key}") String adminApiKey) {
        this.apiKeyRepository = apiKeyRepository;
        this.secretHasher = secretHasher;
        this.adminApiKey = adminApiKey;
    }

    @Transactional
    public Optional<ApiKeyAuthenticationToken> authenticate(String presentedToken, String clientIp) {
        if (!StringUtils.hasText(presentedToken)) {
            return Optional.empty();
        }

        if (constantTimeEquals(presentedToken, adminApiKey)) {
            return Optional.of(new ApiKeyAuthenticationToken(
                    MerchantPrincipal.admin("platform-admin"),
                    authorities(SecurityRoles.ADMIN, SecurityRoles.READ, SecurityRoles.WRITE)));
        }

        int dot = presentedToken.indexOf('.');
        if (dot <= 0 || dot == presentedToken.length() - 1) {
            throw new BadCredentialsException("Malformed API key");
        }
        String keyId = presentedToken.substring(0, dot);
        String secret = presentedToken.substring(dot + 1);

        ApiKey key = apiKeyRepository.findByKeyId(keyId)
                .orElseThrow(() -> new BadCredentialsException("Unknown API key"));

        if (key.getStatus() != ApiKeyStatus.ACTIVE) {
            throw new BadCredentialsException("API key is not active");
        }
        if (key.getExpiresAt() != null && key.getExpiresAt().isBefore(Instant.now())) {
            throw new BadCredentialsException("API key has expired");
        }
        if (!secretHasher.matches(secret, key.getSecretHash())) {
            throw new BadCredentialsException("Invalid API key secret");
        }
        if (!ipAllowed(key.getIpAllowlist(), clientIp)) {
            throw new BadCredentialsException("Client IP not permitted for this API key");
        }

        apiKeyRepository.touchLastUsed(keyId, Instant.now());

        List<GrantedAuthority> auths = key.getKeyType() == ApiKeyType.READ_ONLY
                ? authorities(SecurityRoles.MERCHANT, SecurityRoles.READ)
                : authorities(SecurityRoles.MERCHANT, SecurityRoles.READ, SecurityRoles.WRITE);

        MerchantPrincipal principal = MerchantPrincipal.merchant(key.getMerchantId(), keyId, key.getKeyType());
        return Optional.of(new ApiKeyAuthenticationToken(principal, auths));
    }

    private boolean ipAllowed(String allowlist, String clientIp) {
        if (!StringUtils.hasText(allowlist)) {
            return true;  // no restriction
        }
        List<String> allowed = Arrays.stream(allowlist.split(",")).map(String::trim).toList();
        return allowed.contains("*") || (clientIp != null && allowed.contains(clientIp));
    }

    private List<GrantedAuthority> authorities(String... roles) {
        return Arrays.stream(roles)
                .map(r -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + r))
                .toList();
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
