package com.paymentprocessor.limit.exception;

/**
 * Thrown when an operation is attempted on a reservation whose current status does
 * not permit it (e.g. committing an already-released reservation).
 */
public class InvalidReservationStateException extends RuntimeException {
    public InvalidReservationStateException(String message) {
        super(message);
    }
}
