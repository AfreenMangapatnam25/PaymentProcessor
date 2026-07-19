package com.paymentprocessor.gatewayservice.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.paymentprocessor.gatewayservice.config.GatewayProperties;

import reactor.core.publisher.Mono;

/**
 * Validates a presented API key against the configured key store using a
 * constant-time comparison to avoid leaking timing information. On success it
 * returns a fully authenticated token carrying the principal, merchant id and
 * granted roles.
 */
public class ApiKeyAuthenticationManager implements ReactiveAuthenticationManager {

    private final Map<String, GatewayProperties.ApiKey.Entry> keyIndex;

    public ApiKeyAuthenticationManager(GatewayProperties properties) {
        this.keyIndex = properties.getApikey().getKeys().stream()
                .filter(e -> e.getKey() != null && !e.getKey().isBlank())
                .collect(Collectors.toMap(GatewayProperties.ApiKey.Entry::getKey, e -> e, (a, b) -> a));
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        if (!(authentication instanceof ApiKeyAuthenticationToken token) || token.getApiKey() == null) {
            return Mono.empty();
        }
        GatewayProperties.ApiKey.Entry match = lookupConstantTime(token.getApiKey());
        if (match == null) {
            return Mono.error(new BadCredentialsException("Invalid API key"));
        }
        List<GrantedAuthority> authorities = match.getRoles().stream()
                .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        String principal = match.getPrincipal() != null ? match.getPrincipal() : "api-key-client";
        return Mono.just(new ApiKeyAuthenticationToken(principal, match.getMerchantId(), authorities));
    }

    /**
     * Compare against every configured key with a constant-time equality check so a
     * caller cannot distinguish "no such key" from "wrong key" by response timing.
     */
    private GatewayProperties.ApiKey.Entry lookupConstantTime(String presented) {
        byte[] presentedBytes = presented.getBytes(StandardCharsets.UTF_8);
        GatewayProperties.ApiKey.Entry found = null;
        for (Map.Entry<String, GatewayProperties.ApiKey.Entry> e : keyIndex.entrySet()) {
            byte[] candidate = e.getKey().getBytes(StandardCharsets.UTF_8);
            if (MessageDigest.isEqual(candidate, presentedBytes)) {
                found = e.getValue();
            }
        }
        return found;
    }
}
