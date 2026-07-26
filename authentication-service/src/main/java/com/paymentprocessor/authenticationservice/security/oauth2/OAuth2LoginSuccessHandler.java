package com.paymentprocessor.authenticationservice.security.oauth2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentprocessor.authenticationservice.config.AuthProperties;
import com.paymentprocessor.authenticationservice.domain.SocialProvider;
import com.paymentprocessor.authenticationservice.dto.TokenResponse;
import com.paymentprocessor.authenticationservice.service.SocialLoginService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Terminates the social-login redirect flow by exchanging the provider-authenticated
 * principal for this platform's own JWT.
 *
 * <p>Spring Security has, by this point, already done all of the OAuth2 work: redirected
 * the user to Google/GitHub/Microsoft, received their consent, swapped the authorization
 * code for provider tokens, and loaded the userinfo. This handler is the last step —
 * it maps that external identity onto a local one and hands back a first-party token.
 *
 * <h2>Two response modes</h2>
 * <ul>
 *   <li><b>Redirect mode</b> (default, when {@code auth.social.redirect-uri} is set) —
 *       the browser is 302'd back to the SPA with the tokens as query parameters. This
 *       is the mode a browser-based login needs, because the user arrives here as a
 *       top-level navigation, not an XHR.</li>
 *   <li><b>JSON mode</b> (when no redirect URI is configured) — the tokens are written
 *       directly as a JSON body. Useful for testing the flow with curl and for
 *       non-browser clients.</li>
 * </ul>
 *
 * <h2>Note on putting tokens in a query string</h2>
 * Query parameters land in browser history and, historically, {@code Referer} headers.
 * The mitigation used here is the short access-token TTL (15 minutes by default) and
 * the expectation that the SPA immediately strips them from the URL
 * ({@code history.replaceState}) after reading. A hardened production deployment
 * should prefer setting the refresh token as a {@code HttpOnly; Secure; SameSite=Lax}
 * cookie and returning only a short-lived code the SPA exchanges — that change is
 * localised to this class.
 */
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginSuccessHandler.class);

    private final SocialLoginService socialLoginService;
    private final ObjectMapper objectMapper;
    private final AuthProperties props;

    /**
     * @param socialLoginService resolves the local identity and issues platform tokens
     * @param objectMapper       serialises the token response in JSON mode
     * @param props              supplies {@code auth.social.redirect-uri}
     */
    public OAuth2LoginSuccessHandler(SocialLoginService socialLoginService,
                                     ObjectMapper objectMapper,
                                     AuthProperties props) {
        this.socialLoginService = socialLoginService;
        this.objectMapper = objectMapper;
        this.props = props;
    }

    /**
     * Called by Spring Security once the provider handshake has succeeded.
     *
     * @param request        the callback request from the provider redirect
     * @param response       the response used to redirect or write JSON
     * @param authentication the {@link OAuth2AuthenticationToken} holding the provider
     *                       principal and the registration id that produced it
     * @throws IOException if writing the response fails
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
            // Should be unreachable: this handler is only wired into oauth2Login().
            log.warn("OAuth2 success handler received unexpected authentication type: {}",
                    authentication == null ? "null" : authentication.getClass().getName());
            respondError(response, "unexpected_authentication_type");
            return;
        }

        try {
            // registrationId is "google" | "github" | "microsoft" - it tells us which
            // provider's attribute shape we are about to normalise.
            SocialProvider provider =
                    SocialProvider.fromRegistrationId(oauthToken.getAuthorizedClientRegistrationId());
            OAuth2User principal = oauthToken.getPrincipal();

            SocialUserProfile profile = SocialUserProfile.from(provider, principal);
            TokenResponse tokens = socialLoginService.completeLogin(profile);

            String redirectUri = props.getSocial().getRedirectUri();
            if (redirectUri == null || redirectUri.isBlank()) {
                respondJson(response, tokens);
            } else {
                respondRedirect(response, redirectUri, tokens);
            }

        } catch (SocialLoginService.SocialLoginDeniedException e) {
            log.warn("Social login denied: {}", e.getMessage());
            respondError(response, "account_unavailable");
        } catch (IllegalStateException | IllegalArgumentException e) {
            log.warn("Social login failed during profile normalisation: {}", e.getMessage());
            respondError(response, "invalid_provider_profile");
        }
    }

    /**
     * Redirect mode: sends the browser back to the configured SPA callback with the
     * tokens attached as query parameters.
     *
     * @param response    the servlet response
     * @param redirectUri the configured SPA callback URI
     * @param tokens      the freshly issued platform tokens
     * @throws IOException if the redirect cannot be written
     */
    private void respondRedirect(HttpServletResponse response, String redirectUri, TokenResponse tokens)
            throws IOException {
        String target = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("access_token", enc(tokens.accessToken()))
                .queryParam("refresh_token", enc(tokens.refreshToken()))
                .queryParam("token_type", enc(tokens.tokenType()))
                .queryParam("expires_in", tokens.expiresIn())
                .queryParam("session_id", enc(tokens.sessionId()))
                .build(true)
                .toUriString();
        response.sendRedirect(target);
    }

    /**
     * JSON mode: writes the token response directly to the body. Used when no SPA
     * redirect URI is configured, which makes the flow testable end-to-end with curl.
     *
     * @param response the servlet response
     * @param tokens   the freshly issued platform tokens
     * @throws IOException if the body cannot be written
     */
    private void respondJson(HttpServletResponse response, TokenResponse tokens) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), tokens);
    }

    /**
     * Reports a post-handshake failure. Mirrors the configured response mode so a
     * browser-based client is returned to the SPA with an {@code error} parameter it can
     * render, rather than being left on a blank error page.
     *
     * @param response the servlet response
     * @param code     a stable, non-sensitive error code
     * @throws IOException if the response cannot be written
     */
    private void respondError(HttpServletResponse response, String code) throws IOException {
        String redirectUri = props.getSocial().getRedirectUri();
        if (redirectUri == null || redirectUri.isBlank()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"" + code + "\"}");
            return;
        }
        response.sendRedirect(UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("error", code)
                .build(true)
                .toUriString());
    }

    /**
     * URL-encodes a value for safe inclusion in the redirect query string.
     *
     * @param value the raw value, possibly null
     * @return the encoded value, or an empty string when null
     */
    private static String enc(String value) {
        return value == null ? "" : URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
