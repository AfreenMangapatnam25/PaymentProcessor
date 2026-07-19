package com.paymentprocessor.authenticationservice.event;

import java.time.Instant;
import java.util.UUID;

/** Domain event payloads published to Kafka for Audit, Notification and Risk consumers. */
public final class AuthEvents {

    private AuthEvents() {}

    public interface AuthEvent {
        String eventId();
        String identityId();
        Instant occurredAt();
    }

    public record UserLoggedIn(String eventId, String identityId, String principalType,
                               String deviceId, String ip, Instant occurredAt) implements AuthEvent {
        public static UserLoggedIn of(String identityId, String principalType, String deviceId, String ip) {
            return new UserLoggedIn(newId(), identityId, principalType, deviceId, ip, Instant.now());
        }
    }

    public record UserLoggedOut(String eventId, String identityId, String sessionId,
                                boolean allSessions, Instant occurredAt) implements AuthEvent {
        public static UserLoggedOut of(String identityId, String sessionId, boolean allSessions) {
            return new UserLoggedOut(newId(), identityId, sessionId, allSessions, Instant.now());
        }
    }

    public record PasswordChanged(String eventId, String identityId, boolean viaReset,
                                  Instant occurredAt) implements AuthEvent {
        public static PasswordChanged of(String identityId, boolean viaReset) {
            return new PasswordChanged(newId(), identityId, viaReset, Instant.now());
        }
    }

    public record MfaEnabled(String eventId, String identityId, String kind,
                             Instant occurredAt) implements AuthEvent {
        public static MfaEnabled of(String identityId, String kind) {
            return new MfaEnabled(newId(), identityId, kind, Instant.now());
        }
    }

    public record AccountLocked(String eventId, String identityId, String reason,
                                Instant lockedUntil, Instant occurredAt) implements AuthEvent {
        public static AccountLocked of(String identityId, String reason, Instant lockedUntil) {
            return new AccountLocked(newId(), identityId, reason, lockedUntil, Instant.now());
        }
    }

    public record NewDeviceLogin(String eventId, String identityId, String deviceId, String ip,
                                 Instant occurredAt) implements AuthEvent {
        public static NewDeviceLogin of(String identityId, String deviceId, String ip) {
            return new NewDeviceLogin(newId(), identityId, deviceId, ip, Instant.now());
        }
    }

    private static String newId() {
        return UUID.randomUUID().toString();
    }
}
