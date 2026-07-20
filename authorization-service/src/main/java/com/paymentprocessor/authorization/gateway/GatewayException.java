package com.paymentprocessor.authorization.gateway;

import com.paymentprocessor.authorization.exception.ServiceException;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Wraps failures returned by (or while communicating with) an external payment gateway.
 * {@link #retryable} distinguishes transient network/5xx failures from permanent gateway rejections.
 */
@Getter
public class GatewayException extends ServiceException {

    private final boolean retryable;

    public GatewayException(String message, boolean retryable) {
        super(HttpStatus.BAD_GATEWAY, "GATEWAY_ERROR", message);
        this.retryable = retryable;
    }

    public GatewayException(String message, boolean retryable, Throwable cause) {
        super(HttpStatus.BAD_GATEWAY, "GATEWAY_ERROR", message, cause);
        this.retryable = retryable;
    }
}
