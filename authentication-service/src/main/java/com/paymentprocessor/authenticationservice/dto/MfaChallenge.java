package com.paymentprocessor.authenticationservice.dto;

import java.util.List;

public record MfaChallenge(String mfaToken, List<String> methods) {
}
