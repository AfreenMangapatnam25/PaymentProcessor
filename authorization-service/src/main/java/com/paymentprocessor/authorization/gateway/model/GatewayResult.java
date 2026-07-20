package com.paymentprocessor.authorization.gateway.model;

import com.paymentprocessor.authorization.domain.enums.AuthorizationStatus;
import com.paymentprocessor.authorization.domain.enums.AvsResult;
import com.paymentprocessor.authorization.domain.enums.CardNetwork;
import com.paymentprocessor.authorization.domain.enums.CvvResult;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

/**
 * Provider-neutral, normalized result of a gateway operation.
 */
@Value
@Builder(toBuilder = true)
public class GatewayResult {
    AuthorizationStatus status;
    String gatewayAuthorizationId;
    String authorizationCode;
    String networkReferenceId;
    BigDecimal approvedAmount;
    BigDecimal capturedAmount;
    String responseCode;
    String responseMessage;
    AvsResult avsResult;
    CvvResult cvvResult;
    boolean requiresAuthentication;
    String authenticationUrl;
    CardNetwork cardNetwork;
    String cardBin;
    String cardLast4;
    Integer cardExpMonth;
    Integer cardExpYear;
    String rawResponse;
}
