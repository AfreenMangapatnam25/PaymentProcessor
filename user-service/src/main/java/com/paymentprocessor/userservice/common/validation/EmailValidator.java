package com.paymentprocessor.userservice.common.validation;

import java.util.regex.Pattern;

/**
 * Stateless utility class for email validation and normalization.
 * Not a Spring component - use as static utility or with custom annotations.
 */
public final class EmailValidator {

    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

    private static final String GMAIL_DOMAIN = "gmail.com";
    private static final String GOOGLEMAIL_DOMAIN = "googlemail.com";

    private EmailValidator() {
        // Private constructor to prevent instantiation
    }

    public static boolean validate(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public static String normalize(String email) {
        if (email == null || email.isBlank()) {
            return email;
        }

        String normalized = email.trim().toLowerCase();

        // Handle Gmail normalization
        if (normalized.contains(GMAIL_DOMAIN) || normalized.contains(GOOGLEMAIL_DOMAIN)) {
            String localPart = normalized.substring(0, normalized.indexOf('@'));
            String domain = GMAIL_DOMAIN;

            // Remove dots from local part
            localPart = localPart.replace(".", "");

            // Handle plus addressing
            int plusIndex = localPart.indexOf('+');
            if (plusIndex > 0) {
                localPart = localPart.substring(0, plusIndex);
            }

            normalized = localPart + "@" + domain;
        }

        return normalized;
    }

    public static boolean isValidAndNormalized(String email) {
        String normalized = normalize(email);
        return normalized != null && validate(normalized);
    }
}