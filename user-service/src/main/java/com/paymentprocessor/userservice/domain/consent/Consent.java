package com.paymentprocessor.userservice.domain.consent;

import com.paymentprocessor.userservice.common.exception.ValidationException;
import com.paymentprocessor.userservice.domain.valueobject.ConsentId;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * Consent aggregate root: the current lawful-basis state for one
 * {@code (subject, kind)} pair. No PII. Business behavior (rule 4): grant and
 * revoke are idempotent state transitions that maintain the granted flag and
 * the grant/revoke timestamps together, and record which policy version the
 * subject agreed to.
 */
@Getter
@Builder(toBuilder = true)
public class Consent {

    private final ConsentId id;
    private final ConsentSubject subject;
    private final ConsentKind kind;
    private boolean granted;
    private String source;
    private String policyVersion;
    private Instant grantedAt;
    private Instant revokedAt;
    private final Instant createdAt;
    private Instant updatedAt;
    @Builder.Default
    private long version = 0L;

    public static Consent grantNew(ConsentId id,
                                    ConsentSubject subject,
                                    ConsentKind kind,
                                    String source,
                                    String policyVersion,
                                    Instant now) {
        Consent consent = Consent.builder()
                .id(id)
                .subject(subject)
                .kind(kind)
                .createdAt(now)
                .updatedAt(now)
                .version(0L)
                .build();
        consent.grant(source, policyVersion, now);
        return consent;
    }

    public void grant(String source, String policyVersion, Instant now) {
        this.granted = true;
        this.source = source;
        this.policyVersion = policyVersion;
        this.grantedAt = now;
        this.revokedAt = null;
        this.updatedAt = now;
    }

    public void revoke(Instant now) {
        if (!granted) {
            // Idempotent: already revoked.
            this.updatedAt = now;
            return;
        }
        this.granted = false;
        this.revokedAt = now;
        this.updatedAt = now;
    }

    public boolean isGranted() {
        return granted;
    }

    /** Guard used by callers that require an active grant (e.g. before marketing). */
    public void assertGranted() {
        if (!granted) {
            throw new ValidationException("consent", "Consent for " + kind + " is not granted");
        }
    }
}
