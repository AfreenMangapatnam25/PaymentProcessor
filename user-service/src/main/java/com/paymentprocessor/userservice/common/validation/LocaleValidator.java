package com.paymentprocessor.userservice.common.validation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Stateless utility class for locale validation.
 * Validates Java Locale formats (language, country, variant).
 */
public final class LocaleValidator {

    private static final String LANGUAGE_REGEX = "^[a-z]{2,3}$";
    private static final String COUNTRY_REGEX = "^[A-Z]{2}$";
    private static final String VARIANT_REGEX = "^[A-Za-z0-9_]+$";

    private static final Set<String> SUPPORTED_LANGUAGES = new HashSet<>(Arrays.asList(
            "en", "es", "fr", "de", "it", "pt", "ru", "zh", "ja", "ko",
            "ar", "hi", "bn", "pa", "mr", "ur", "fa", "ta", "te", "ml"
    ));

    private LocaleValidator() {
        // Private constructor to prevent instantiation
    }

    public static boolean validateLocale(String localeString) {
        if (localeString == null || localeString.isBlank()) {
            return false;
        }

        try {
            String[] parts = localeString.trim().split("_");

            // Must have at least language
            if (parts.length < 1 || !validateLanguage(parts[0])) {
                return false;
            }

            // Validate country if present
            if (parts.length >= 2 && !validateCountry(parts[1])) {
                return false;
            }

            // Validate variant if present
            if (parts.length >= 3 && !validateVariant(parts[2])) {
                return false;
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean validateLanguage(String language) {
        if (language == null || language.isBlank()) {
            return false;
        }
        return language.matches(LANGUAGE_REGEX) && SUPPORTED_LANGUAGES.contains(language.toLowerCase());
    }

    public static boolean validateCountry(String country) {
        if (country == null || country.isBlank()) {
            return false;
        }
        return country.matches(COUNTRY_REGEX) && CountryValidator.isAlpha2(country);
    }

    public static boolean validateVariant(String variant) {
        if (variant == null || variant.isBlank()) {
            return false;
        }
        return variant.matches(VARIANT_REGEX);
    }

    public static String normalize(String localeString) {
        if (localeString == null || localeString.isBlank()) {
            return null;
        }

        try {
            String[] parts = localeString.trim().split("_");

            if (parts.length == 0) {
                return null;
            }

            // Normalize language (lowercase)
            String language = parts[0].toLowerCase();
            if (!validateLanguage(language)) {
                return null;
            }

            if (parts.length == 1) {
                return language;
            }

            // Normalize country (uppercase)
            String country = parts[1].toUpperCase();
            if (!validateCountry(country)) {
                country = null;
            }

            if (parts.length == 2) {
                return country != null ? language + "_" + country : language;
            }

            // Normalize variant
            String variant = parts[2];
            if (!validateVariant(variant)) {
                variant = null;
            }

            if (country != null && variant != null) {
                return language + "_" + country + "_" + variant;
            } else if (country != null) {
                return language + "_" + country;
            } else {
                return language;
            }
        } catch (Exception e) {
            return null;
        }
    }

    public static Locale toLocale(String localeString) {
        String normalized = normalize(localeString);
        if (normalized == null) {
            return null;
        }

        String[] parts = normalized.split("_");
        if (parts.length == 1) {
            return new Locale(parts[0]);
        } else if (parts.length == 2) {
            return new Locale(parts[0], parts[1]);
        } else if (parts.length >= 3) {
            return new Locale(parts[0], parts[1], parts[2]);
        }
        return null;
    }
}
