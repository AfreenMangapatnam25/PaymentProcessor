package com.paymentprocessor.auditservice.batch;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.paymentprocessor.auditservice.config.AuditProperties;

/**
 * Anchors the daily root hash <em>outside</em> this system so that even someone who
 * could rewrite both MongoDB and the S3 batch cannot forge history undetectably: the
 * externally recorded root would no longer match.
 *
 * <p>Two modes:
 * <ul>
 *   <li>{@code log} — writes the root to the application log (dev / minimal).</li>
 *   <li>{@code http} — POSTs the root to a timestamping authority / notary / ledger and
 *       returns its reference. This is what production should use.</li>
 * </ul>
 * The returned reference is persisted on the batch manifest.
 */
@Component
public class ExternalAnchorService {

    private static final Logger log = LoggerFactory.getLogger(ExternalAnchorService.class);

    private final String mode;
    private final String endpoint;
    private final HttpClient httpClient;

    public ExternalAnchorService(AuditProperties props) {
        this.mode = props.getAnchor().getMode();
        this.endpoint = props.getAnchor().getEndpoint();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    /**
     * Anchors the root and returns an opaque reference (e.g. notary receipt id, ledger
     * tx id, or a log marker).
     */
    public String anchor(LocalDate batchDate, String rootHash) {
        if ("http".equalsIgnoreCase(mode) && StringUtils.hasText(endpoint)) {
            return anchorHttp(batchDate, rootHash);
        }
        String ref = "log:" + batchDate + ":" + rootHash;
        log.info("ANCHOR batchDate={} root={} (mode=log)", batchDate, rootHash);
        return ref;
    }

    private String anchorHttp(LocalDate batchDate, String rootHash) {
        String body = "{\"batchDate\":\"" + batchDate + "\",\"root\":\"" + rootHash + "\"}";
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException(
                        "Anchor endpoint returned HTTP " + response.statusCode());
            }
            log.info("Anchored batchDate={} root={} via {} -> {}",
                    batchDate, rootHash, endpoint, response.body());
            return "http:" + response.body();
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to anchor batch root externally", e);
        }
    }
}
