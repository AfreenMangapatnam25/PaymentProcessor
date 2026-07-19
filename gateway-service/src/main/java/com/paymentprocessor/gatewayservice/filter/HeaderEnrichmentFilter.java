package com.paymentprocessor.gatewayservice.filter;

import java.util.stream.Collectors;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

import com.paymentprocessor.gatewayservice.security.ApiKeyAuthenticationToken;

import reactor.core.publisher.Mono;

/**
 * Enriches the outbound request with a trusted identity derived from the verified
 * credential, so downstream services never parse tokens themselves. Inbound copies
 * of these headers are stripped by the gateway's {@code RemoveRequestHeader}
 * default filters, so a client cannot spoof identity.
 */
@Component
public class HeaderEnrichmentFilter implements GlobalFilter, Ordered {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_ROLES_HEADER = "X-User-Roles";
    public static final String MERCHANT_ID_HEADER = "X-Merchant-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .filter(auth -> auth != null && auth.isAuthenticated())
                .map(auth -> enrich(exchange, auth))
                .defaultIfEmpty(exchange)
                .flatMap(chain::filter);
    }

    private ServerWebExchange enrich(ServerWebExchange exchange, Authentication auth) {
        String userId = auth.getName();
        String roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
        String merchantId = resolveMerchantId(auth);

        ServerHttpRequest.Builder builder = exchange.getRequest().mutate();
        if (StringUtils.hasText(userId)) {
            builder.header(USER_ID_HEADER, userId);
        }
        if (StringUtils.hasText(roles)) {
            builder.header(USER_ROLES_HEADER, roles);
        }
        if (StringUtils.hasText(merchantId)) {
            builder.header(MERCHANT_ID_HEADER, merchantId);
        }
        return exchange.mutate().request(builder.build()).build();
    }

    private String resolveMerchantId(Authentication auth) {
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            Object claim = jwt.getClaims().get("merchant_id");
            return claim != null ? String.valueOf(claim) : null;
        }
        if (auth instanceof ApiKeyAuthenticationToken apiKeyAuth) {
            return apiKeyAuth.getMerchantId();
        }
        return null;
    }

    @Override
    public int getOrder() {
        // After correlation/size filters, well before the routing filter proxies out.
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }
}
