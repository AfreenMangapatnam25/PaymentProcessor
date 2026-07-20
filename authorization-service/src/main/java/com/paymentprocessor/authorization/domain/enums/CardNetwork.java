package com.paymentprocessor.authorization.domain.enums;

public enum CardNetwork {
    VISA, MASTERCARD, AMEX, DISCOVER, JCB, DINERS, UNIONPAY, UNKNOWN;

    /** Best-effort network inference from the card BIN (first digits of the PAN). */
    public static CardNetwork fromPan(String pan) {
        if (pan == null || pan.isBlank()) {
            return UNKNOWN;
        }
        String p = pan.trim();
        if (p.startsWith("4")) return VISA;
        if (p.matches("^(5[1-5]|2[2-7]).*")) return MASTERCARD;
        if (p.matches("^3[47].*")) return AMEX;
        if (p.matches("^(6011|65|64[4-9]).*")) return DISCOVER;
        if (p.matches("^35.*")) return JCB;
        if (p.matches("^3(0[0-5]|6|8).*")) return DINERS;
        if (p.startsWith("62")) return UNIONPAY;
        return UNKNOWN;
    }
}
