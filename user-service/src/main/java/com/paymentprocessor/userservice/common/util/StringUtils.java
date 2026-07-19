package com.paymentprocessor.userservice.common.util;

public class StringUtils {

    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";

    public static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return email;
        }

        String normalized = email.trim().toLowerCase();

        // Remove dots from Gmail addresses
        if (normalized.contains("@gmail.com") || normalized.contains("@googlemail.com")) {
            String localPart = normalized.substring(0, normalized.indexOf('@'));
            String domain = normalized.substring(normalized.indexOf('@'));
            localPart = localPart.replace(".", "");

            // Handle Gmail plus addressing
            int plusIndex = localPart.indexOf('+');
            if (plusIndex > 0) {
                localPart = localPart.substring(0, plusIndex);
            }

            normalized = localPart + domain;
        }

        return normalized;
    }

    public static String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return phone;
        }

        // Remove all non-digit characters
        String digits = phone.replaceAll("\\D", "");

        // If starts with 0, remove it
        if (digits.startsWith("0")) {
            digits = digits.substring(1);
        }

        return digits;
    }

    public static boolean isBlank(String str) {
        return str == null || str.isBlank();
    }

    public static String trim(String str) {
        return str != null ? str.trim() : null;
    }

    public static boolean isValidEmail(String email) {
        return email != null && email.matches(EMAIL_REGEX);
    }

    public static boolean hasText(String str) {
        return str != null && !str.isBlank();
    }
}
