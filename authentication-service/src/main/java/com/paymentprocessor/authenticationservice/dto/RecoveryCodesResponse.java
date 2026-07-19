package com.paymentprocessor.authenticationservice.dto;

import java.util.List;

public record RecoveryCodesResponse(List<String> codes) {
}
