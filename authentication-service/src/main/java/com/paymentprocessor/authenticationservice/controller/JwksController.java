package com.paymentprocessor.authenticationservice.controller;

import com.paymentprocessor.authenticationservice.security.RsaKeyProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Publishes the public JWK set consumed by the gateway-service and other
 * resource servers to verify RS256 access tokens.
 */
@RestController
public class JwksController {

    private final RsaKeyProvider keyProvider;

    public JwksController(RsaKeyProvider keyProvider) {
        this.keyProvider = keyProvider;
    }

    @GetMapping(value = "/.well-known/jwks.json", produces = "application/json")
    public Map<String, Object> jwks() {
        return keyProvider.jwkSetJson();
    }
}
