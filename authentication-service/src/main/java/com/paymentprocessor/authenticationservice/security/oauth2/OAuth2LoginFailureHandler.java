package com.paymentprocessor.authenticationservice.security.oauth2;

import com.paymentprocessor.authenticationservice.config.AuthProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Handles failures during the social-login handshake — the user declining consent at the
 * provider, an expired/replayed authorization code, a state-parameter (CSRF) mismatch,
 * or the provider rejecting our client credentials.
 *
 * <p>Deliberately does not echo the provider's raw error text back to the caller. Those
 * messages occasionally embed client identifiers or internal detail, and a login error
 * page is an unauthenticated surface. A stable, coarse error code is returned instead,
 * while the full cause is logged server-side for operators.
 */
@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginFailureHandler.class);

    private final AuthProperties props;

    /**
     * @param props supplies {@code auth.social.redirect-uri}, which decides whether
     *              failures are reported by redirect or as a JSON body
     */
    public OAuth2LoginFailureHandler(AuthProperties props) {
        this.props = props;
    }

    /**
     * Called by Spring Security when the OAuth2 login flow fails at any point before a
     * principal is established.
     *
     * @param request   the failed callback request
     * @param response  the response used to redirect or write JSON
     * @param exception the underlying authentication failure
     * @throws IOException if writing the response fails
     */
    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {

        String code = "authentication_failed";
        if (exception instanceof OAuth2AuthenticationException oauthException
                && oauthException.getError() != null
                && oauthException.getError().getErrorCode() != null) {
            // e.g. "access_denied" when the user clicks Cancel at the provider.
            code = oauthException.getError().getErrorCode();
        }

        log.warn("Social login failed [{}]: {}", code, exception.getMessage());

        String redirectUri = props.getSocial().getRedirectUri();
        if (redirectUri == null || redirectUri.isBlank()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"" + sanitise(code) + "\"}");
            return;
        }

        response.sendRedirect(UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("error", sanitise(code))
                .build(true)
                .toUriString());
    }

    /**
     * Strips anything outside a conservative character set so a provider-supplied error
     * code can never break out of the JSON body or the redirect query string.
     *
     * @param code the raw error code from the provider
     * @return a safe, bounded-length code
     */
    private static String sanitise(String code) {
        String cleaned = code.replaceAll("[^a-zA-Z0-9_\\-]", "");
        return cleaned.length() > 64 ? cleaned.substring(0, 64) : cleaned;
    }
}
