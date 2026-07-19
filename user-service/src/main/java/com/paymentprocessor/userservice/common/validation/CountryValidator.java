package com.paymentprocessor.userservice.common.validation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Stateless utility class for country validation.
 * Validates ISO country codes (alpha-2 and alpha-3).
 */
public final class CountryValidator {

    private static final Set<String> ISO_ALPHA_2_COUNTRIES = new HashSet<>(Arrays.asList(
            "AF", "AL", "DZ", "AS", "AD", "AO", "AI", "AQ", "AG", "AR",
            "AM", "AW", "AU", "AT", "AZ", "BS", "BH", "BD", "BB", "BY",
            "BE", "BZ", "BJ", "BM", "BT", "BO", "BA", "BW", "BV", "BR",
            "IO", "BN", "BG", "BF", "BI", "KH", "CM", "CA", "CV", "KY",
            "CF", "TD", "CL", "CN", "CX", "CC", "CO", "KM", "CG", "CD",
            "CK", "CR", "CI", "HR", "CU", "CY", "CZ", "DK", "DJ", "DM",
            "DO", "EC", "EG", "SV", "GQ", "ER", "EE", "ET", "FK", "FO",
            "FJ", "FI", "FR", "GF", "PF", "TF", "GA", "GM", "GE", "DE",
            "GH", "GI", "GR", "GL", "GD", "GP", "GU", "GT", "GG", "GN",
            "GW", "GY", "HT", "HM", "VA", "HN", "HK", "HU", "IS", "IN",
            "ID", "IR", "IQ", "IE", "IM", "IL", "IT", "JM", "JP", "JE",
            "JO", "KZ", "KE", "KI", "KP", "KR", "KW", "KG", "LA", "LV",
            "LB", "LS", "LR", "LY", "LI", "LT", "LU", "MO", "MG", "MW",
            "MY", "MV", "ML", "MT", "MH", "MQ", "MR", "MU", "YT", "MX",
            "FM", "MD", "MC", "MN", "ME", "MS", "MA", "MZ", "MM", "NA",
            "NR", "NP", "NL", "AN", "NC", "NZ", "NI", "NE", "NG", "NU",
            "NF", "MK", "MP", "NO", "OM", "PK", "PW", "PS", "PA", "PG",
            "PY", "PE", "PH", "PN", "PL", "PT", "PR", "QA", "RE", "RO",
            "RU", "RW", "BL", "SH", "KN", "LC", "MF", "PM", "VC", "WS",
            "SM", "ST", "SA", "SN", "RS", "SC", "SL", "SG", "SK", "SI",
            "SB", "SO", "ZA", "GS", "SS", "ES", "LK", "SD", "SR", "SJ",
            "SZ", "SE", "CH", "SY", "TW", "TJ", "TZ", "TH", "TL", "TG",
            "TK", "TO", "TT", "TN", "TR", "TM", "TC", "TV", "UG", "UA",
            "AE", "GB", "US", "UM", "UY", "UZ", "VU", "VE", "VN", "VG",
            "VI", "WF", "EH", "YE", "ZM", "ZW"
    ));

    private static final Set<String> ISO_ALPHA_3_COUNTRIES = new HashSet<>(Arrays.asList(
            "AFG", "ALB", "DZA", "ASM", "AND", "AGO", "AIA", "ATA", "ATG", "ARG",
            "ARM", "ABW", "AUS", "AUT", "AZE", "BHS", "BHR", "BGD", "BRB", "BLR",
            "BEL", "BLZ", "BEN", "BMU", "BTN", "BOL", "BIH", "BWA", "BVT", "BRA",
            "IOT", "BRN", "BGR", "BFA", "BDI", "CPV", "KHM", "CMR", "CAN", "CYM",
            "CAF", "TCD", "CHL", "CHN", "CXR", "CCK", "COL", "COM", "COG", "COD",
            "COK", "CRI", "CIV", "HRV", "CUB", "CYP", "CZE", "DNK", "DJI", "DMA",
            "DOM", "ECU", "EGY", "SLV", "GNQ", "ERI", "EST", "SWZ", "ETH", "FLK",
            "FRO", "FJI", "FIN", "FRA", "GUF", "PYF", "ATF", "GAB", "GMB", "GEO",
            "DEU", "GHA", "GIB", "GRC", "GRL", "GRD", "GLP", "GUM", "GTM", "GGY",
            "GIN", "GNB", "GUY", "HTI", "HMD", "VAT", "HND", "HKG", "HUN", "ISL",
            "IND", "IDN", "IRN", "IRQ", "IRL", "IMN", "ISR", "ITA", "JAM", "JPN",
            "JEY", "JOR", "KAZ", "KEN", "KIR", "PRK", "KOR", "KWT", "KGZ", "LAO",
            "LVA", "LBN", "LSO", "LBR", "LBY", "LIE", "LTU", "LUX", "MAC", "MDG",
            "MWI", "MYS", "MDV", "MLI", "MLT", "MHL", "MTQ", "MRT", "MUS", "MYT",
            "MEX", "FSM", "MDA", "MCO", "MNG", "MNE", "MSR", "MAR", "MOZ", "MMR",
            "NAM", "NRU", "NPL", "NLD", "ANT", "NCL", "NZL", "NIC", "NER", "NGA",
            "NIU", "NFK", "MKD", "MNP", "NOR", "OMN", "PAK", "PLW", "PAN", "PNG",
            "PRY", "PER", "PHL", "PCN", "POL", "PRT", "PRI", "QAT", "REU", "ROU",
            "RUS", "RWA", "BLM", "SHN", "KNA", "LCA", "MAF", "SPM", "VCT", "WSM",
            "SMR", "STP", "SAU", "SEN", "SRB", "SYC", "SLE", "SGP", "SVK", "SVN",
            "SLB", "SOM", "ZAF", "SGS", "SSD", "ESP", "LKA", "SDN", "SUR", "SJM",
            "SWE", "CHE", "SYR", "TWN", "TJK", "TZA", "THA", "TLS", "TGO", "TKL",
            "TON", "TTO", "TUN", "TUR", "TKM", "TCA", "TUV", "UGA", "UKR", "ARE",
            "GBR", "USA", "UMI", "URY", "UZB", "VUT", "VEN", "VNM", "VGB", "VIR",
            "WLF", "ESH", "YEM", "ZMB", "ZWE"
    ));

    private CountryValidator() {
        // Private constructor to prevent instantiation
    }

    public static boolean validateIsoCountry(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            return false;
        }

        String code = countryCode.trim().toUpperCase();

        // Check alpha-2 (2 characters)
        if (code.length() == 2) {
            return ISO_ALPHA_2_COUNTRIES.contains(code);
        }

        // Check alpha-3 (3 characters)
        if (code.length() == 3) {
            return ISO_ALPHA_3_COUNTRIES.contains(code);
        }

        return false;
    }

    public static boolean isAlpha2(String countryCode) {
        if (countryCode == null || countryCode.length() != 2) {
            return false;
        }
        return ISO_ALPHA_2_COUNTRIES.contains(countryCode.toUpperCase());
    }

    public static boolean isAlpha3(String countryCode) {
        if (countryCode == null || countryCode.length() != 3) {
            return false;
        }
        return ISO_ALPHA_3_COUNTRIES.contains(countryCode.toUpperCase());
    }

    public static String normalizeToAlpha2(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            return null;
        }

        String code = countryCode.trim().toUpperCase();

        if (code.length() == 2 && ISO_ALPHA_2_COUNTRIES.contains(code)) {
            return code;
        }

        if (code.length() == 3 && ISO_ALPHA_3_COUNTRIES.contains(code)) {
            // Convert alpha-3 to alpha-2
            for (String alpha2 : ISO_ALPHA_2_COUNTRIES) {
                // This is a simplified mapping - in production use a proper mapping
                if (code.equals(getAlpha3ForAlpha2(alpha2))) {
                    return alpha2;
                }
            }
        }

        return null;
    }

    private static String getAlpha3ForAlpha2(String alpha2) {
        // Simplified mapping - use a proper mapping in production
        return switch (alpha2) {
            case "US" -> "USA";
            case "GB" -> "GBR";
            case "CA" -> "CAN";
            case "AU" -> "AUS";
            case "IN" -> "IND";
            case "DE" -> "DEU";
            case "FR" -> "FRA";
            case "IT" -> "ITA";
            case "ES" -> "ESP";
            case "JP" -> "JPN";
            case "CN" -> "CHN";
            case "BR" -> "BRA";
            default -> alpha2;
        };
    }
}