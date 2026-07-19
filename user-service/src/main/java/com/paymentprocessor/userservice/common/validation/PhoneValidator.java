package com.paymentprocessor.userservice.common.validation;

import java.util.regex.Pattern;

/**
 * Stateless utility class for phone validation and normalization.
 * Supports E.164 format validation.
 */
public final class PhoneValidator {

    private static final String E164_REGEX = "^\\+?[1-9]\\d{1,14}$";
    private static final Pattern E164_PATTERN = Pattern.compile(E164_REGEX);

    private static final String DIGITS_ONLY_REGEX = "^\\d{7,15}$";
    private static final Pattern DIGITS_ONLY_PATTERN = Pattern.compile(DIGITS_ONLY_REGEX);

    private PhoneValidator() {
        // Private constructor to prevent instantiation
    }

    public static boolean validateE164(String phone) {
        if (phone == null || phone.isBlank()) {
            return false;
        }
        return E164_PATTERN.matcher(phone.trim()).matches();
    }

    public static String normalize(String phone) {
        if (phone == null || phone.isBlank()) {
            return phone;
        }

        String normalized = phone.trim();

        // Remove all non-digit characters except leading '+'
        if (normalized.startsWith("+")) {
            String digits = normalized.substring(1).replaceAll("\\D", "");
            normalized = "+" + digits;
        } else {
            normalized = normalized.replaceAll("\\D", "");
        }

        return normalized;
    }

    public static boolean validateNormalizedE164(String phone) {
        String normalized = normalize(phone);
        return normalized != null && validateE164(normalized);
    }

    public static boolean isValidDigitsOnly(String phone) {
        if (phone == null || phone.isBlank()) {
            return false;
        }
        return DIGITS_ONLY_PATTERN.matcher(phone.trim()).matches();
    }
}