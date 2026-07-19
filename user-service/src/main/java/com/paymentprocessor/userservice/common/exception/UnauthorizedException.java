package com.paymentprocessor.userservice.common.exception;

import com.paymentprocessor.userservice.common.constants.ErrorCode;
import org.springframework.http.HttpStatus;

public class UnauthorizedException extends  ApplicationException{

    protected UnauthorizedException(ErrorCode errorCode, HttpStatus httpStatus, String message) {
        super(errorCode, httpStatus, message);
    }

    protected UnauthorizedException(ErrorCode errorCode, HttpStatus httpStatus) {
        super(errorCode, httpStatus);
    }

    protected UnauthorizedException(ErrorCode errorCode) {
        super(errorCode);
    }

    protected UnauthorizedException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    protected UnauthorizedException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
