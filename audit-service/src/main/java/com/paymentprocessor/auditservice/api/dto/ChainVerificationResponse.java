package com.paymentprocessor.auditservice.api.dto;

import com.paymentprocessor.auditservice.service.ChainVerificationService.VerificationResult;

public record ChainVerificationResponse(
        boolean valid,
        long fromSeq,
        long toSeq,
        long recordsChecked,
        Long firstBrokenSeq,
        String detail) {

    public static ChainVerificationResponse from(VerificationResult r) {
        return new ChainVerificationResponse(
                r.valid(), r.fromSeq(), r.toSeq(), r.recordsChecked(), r.firstBrokenSeq(), r.detail());
    }
}
