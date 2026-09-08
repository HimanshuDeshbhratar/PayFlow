package com.payflow.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class PayFlowException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public PayFlowException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public static PayFlowException notFound(String message) {
        return new PayFlowException(message, HttpStatus.NOT_FOUND, "NOT_FOUND");
    }

    public static PayFlowException badRequest(String message) {
        return new PayFlowException(message, HttpStatus.BAD_REQUEST, "BAD_REQUEST");
    }

    public static PayFlowException conflict(String message) {
        return new PayFlowException(message, HttpStatus.CONFLICT, "CONFLICT");
    }

    public static PayFlowException unauthorized(String message) {
        return new PayFlowException(message, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
    }

    public static PayFlowException forbidden(String message) {
        return new PayFlowException(message, HttpStatus.FORBIDDEN, "FORBIDDEN");
    }

    public static PayFlowException tooManyRequests(String message) {
        return new PayFlowException(message, HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED");
    }

    public static PayFlowException validation(String message) {
        return new PayFlowException(message, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR");
    }
}
