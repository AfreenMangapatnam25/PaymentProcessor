package com.paymentprocessor.auditservice.service;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.paymentprocessor.auditservice.config.AuditProperties;
import com.paymentprocessor.auditservice.service.exception.PiiDetectedException;

/**
 * Defence-in-depth guard that rejects before/after payloads which appear to contain raw
 * PII. Producers are contractually required to send only redacted references, but this
 * service is the last line of defence: because audit records are exempt from GDPR
 * erasure, a leaked raw identifier here would be unerasable.
 *
 * <p>The guard is heuristic and conservative — it errs toward rejecting rather than
 * silently storing. It checks both field names (keys that should never appear raw) and
 * values (patterns that look like full card numbers, SSNs, emails, IBANs).
 */
@Service
public class PiiGuardService {

    private static final Logger log = LoggerFactory.getLogger(PiiGuardService.class);

    /** Keys whose presence with an unmasked value indicates a redaction failure upstream. */
    private static final Set<String> FORBIDDEN_KEYS = Set.of(
            "pan", "card_number", "cardnumber", "cardno", "cvv", "cvc", "cvv2",
            "ssn", "social_security_number", "tax_id", "taxid", "national_id",
            "password", "secret", "private_key", "full_name", "email", "phone",
            "date_of_birth", "dob", "iban", "account_number", "routing_number");

    // Full PAN: 13-19 digits (Luhn-validated below). Kept loose here, validated by Luhn.
    private static final Pattern DIGIT_RUN = Pattern.compile("\\b\\d[\\d \\-]{11,21}\\d\\b");
    private static final Pattern EMAIL = Pattern.compile(
            "\\b[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}\\b");
    private static final Pattern SSN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");
    private static final Pattern IBAN = Pattern.compile("\\b[A-Z]{2}\\d{2}[A-Z0-9]{11,30}\\b");

    private final boolean enabled;

    public PiiGuardService(AuditProperties props) {
        this.enabled = props.getIngestion().isPiiGuardEnabled();
    }

    /** @throws PiiDetectedException if either payload looks like it contains raw PII. */
    public void assertRedacted(Map<String, Object> before, Map<String, Object> after) {
        if (!enabled) {
            return;
        }
        scan("before", before);
        scan("after", after);
    }

    private void scan(String label, Map<String, Object> payload) {
        if (payload == null) {
            return;
        }
        for (Map.Entry<String, Object> e : payload.entrySet()) {
            scanNode(label, e.getKey(), e.getValue());
        }
    }

    @SuppressWarnings("unchecked")
    private void scanNode(String label, String key, Object value) {
        if (key != null && FORBIDDEN_KEYS.contains(key.toLowerCase()) && !isMasked(value)) {
            reject(label, "forbidden key '" + key + "' with an unmasked value");
        }
        if (value instanceof Map<?, ?> nested) {
            for (Map.Entry<?, ?> e : ((Map<String, Object>) nested).entrySet()) {
                scanNode(label, String.valueOf(e.getKey()), e.getValue());
            }
        } else if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                scanNode(label, key, item);
            }
        } else if (value instanceof String s) {
            scanString(label, key, s);
        }
    }

    private void scanString(String label, String key, String s) {
        if (isMasked(s)) {
            return;
        }
        if (EMAIL.matcher(s).find()) {
            reject(label, "value under '" + key + "' looks like an email address");
        }
        if (SSN.matcher(s).find()) {
            reject(label, "value under '" + key + "' looks like a social security number");
        }
        if (IBAN.matcher(s).find()) {
            reject(label, "value under '" + key + "' looks like an IBAN");
        }
        var m = DIGIT_RUN.matcher(s);
        while (m.find()) {
            String digits = m.group().replaceAll("[ \\-]", "");
            if (digits.length() >= 13 && digits.length() <= 19 && luhnValid(digits)) {
                reject(label, "value under '" + key + "' looks like a full card number");
            }
        }
    }

    /**
     * A value is considered masked if it contains a masking marker (asterisks) or is a
     * short "last4"-style token. This lets legitimate redacted diffs like
     * {@code {"last4":"4242"}} pass.
     */
    private boolean isMasked(Object value) {
        if (!(value instanceof String s)) {
            return false;
        }
        if (s.contains("*") || s.contains("•") || s.matches("(?i).*x{4,}.*")) {
            return true;
        }
        // Pure short tokens (e.g. last4) are not full identifiers.
        return s.length() <= 6;
    }

    private boolean luhnValid(String digits) {
        int sum = 0;
        boolean alternate = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = digits.charAt(i) - '0';
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n -= 9;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }

    private void reject(String label, String reason) {
        // Never log the offending value itself.
        log.warn("PII guard rejected {} payload: {}", label, reason);
        throw new PiiDetectedException("Rejected " + label + ": " + reason
                + ". Audit records must contain only redacted references, never raw PII.");
    }
}
