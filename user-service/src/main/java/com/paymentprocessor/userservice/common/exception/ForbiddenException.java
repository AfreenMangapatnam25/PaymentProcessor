package com.paymentprocessor.userservice.common.exception;

import com.paymentprocessor.userservice.common.constants.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * Access-forbidden (HTTP 403). Public constructor added so tenancy/authorization
 * checks can raise it directly.
 */
public class ForbiddenException extends ApplicationException {

    protected ForbiddenException(ErrorCode errorCode, HttpStatus httpStatus, String message) {
        super(errorCode, httpStatus, message);
    }

    protected ForbiddenException(ErrorCode errorCode, HttpStatus httpStatus) {
        super(errorCode, httpStatus);
    }

    protected ForbiddenException(ErrorCode errorCode) {
        super(errorCode);
    }

    protected ForbiddenException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    protected ForbiddenException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public ForbiddenException(String message) {
        super(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN, message);
    }

    /** Missing or insufficient merchant context on the caller's token. */
    public static ForbiddenException merchantContextRequired() {
        return new ForbiddenException("Merchant context is required for this operation");
    }
}
