package com.paymentprocessor.userservice.common.util;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class MaskingUtils {

    private static final String MASK = "****";
    private static final String EMAIL_MASK = "****@****.***";

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("(\\d{1,3})(\\d{4})(\\d{4})");
    private static final Pattern CARD_TOKEN_PATTERN = Pattern.compile("tok_(.+)_(.+)");
    private static final Pattern ADDRESS_PATTERN = Pattern.compile("(.{0,5})(.{1,})(.{0,5})");

    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return email;
        }

        try {
            var matcher = EMAIL_PATTERN.matcher(email);
            if (matcher.matches()) {
                String domain = matcher.group(1);
                int domainLength = domain.length();
                if (domainLength <= 4) {
                    return "****@" + domain;
                }
                String maskedDomain = domain.substring(0, 2) + "****" + domain.substring(domainLength - 2);
                return "****@" + maskedDomain;
            }
            return MASK;
        } catch (Exception e) {
            return MASK;
        }
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return phone;
        }

        try {
            String digits = phone.replaceAll("\\D", "");
            if (digits.length() < 8) {
                return MASK;
            }

            int totalDigits = digits.length();
            String first = digits.substring(0, Math.min(3, totalDigits - 6));
            String last = digits.substring(totalDigits - 4);
            int maskedLength = totalDigits - first.length() - last.length();

            return first + "*".repeat(maskedLength) + last;
        } catch (Exception e) {
            return MASK;
        }
    }

    public static String maskCardToken(String cardToken) {
        if (cardToken == null || cardToken.isBlank()) {
            return cardToken;
        }

        try {
            var matcher = CARD_TOKEN_PATTERN.matcher(cardToken);
            if (matcher.matches()) {
                String prefix = matcher.group(1);
                String suffix = matcher.group(2);
                int suffixLen = suffix.length();
                if (suffixLen <= 4) {
                    return "tok_" + prefix + "_****";
                }
                String maskedSuffix = suffix.substring(0, 2) + "****" + suffix.substring(suffixLen - 2);
                return "tok_" + prefix + "_" + maskedSuffix;
            }
            return cardToken.length() > 8
                    ? cardToken.substring(0, 4) + "****" + cardToken.substring(cardToken.length() - 4)
                    : MASK;
        } catch (Exception e) {
            return MASK;
        }
    }

    public static String maskAddress(String address) {
        if (address == null || address.isBlank()) {
            return address;
        }

        try {
            if (address.length() <= 8) {
                return MASK;
            }

            String first = address.substring(0, 4);
            String last = address.substring(address.length() - 4);
            int middleLength = address.length() - 8;

            return first + "*".repeat(Math.min(middleLength, 10)) + last;
        } catch (Exception e) {
            return MASK;
        }
    }

    public static String maskPii(String value, PiiType type) {
        if (value == null || value.isBlank()) {
            return value;
        }

        return switch (type) {
            case EMAIL -> maskEmail(value);
            case PHONE -> maskPhone(value);
            case CARD_TOKEN -> maskCardToken(value);
            case ADDRESS -> maskAddress(value);
        };
    }

    public enum PiiType {
        EMAIL,
        PHONE,
        CARD_TOKEN,
        ADDRESS
    }
}
