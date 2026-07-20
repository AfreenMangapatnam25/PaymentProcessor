package com.paymentprocessor.authenticationservice.web;

import com.paymentprocessor.authenticationservice.service.AuthenticationService.RequestContext;
import jakarta.servlet.http.HttpServletRequest;

/** Extracts client metadata (IP, user-agent) honouring reverse-proxy headers. */
public final class HttpRequestUtils {

    private HttpRequestUtils() {}

    public static RequestContext context(HttpServletRequest request) {
        return new RequestContext(clientIp(request), request.getHeader("User-Agent"));
    }

    public static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }
        return request.getRemoteAddr();
    }
}
