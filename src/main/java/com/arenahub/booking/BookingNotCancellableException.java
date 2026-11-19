package com.arenahub.booking;

public class BookingNotCancellableException extends RuntimeException {

    public BookingNotCancellableException(String message) {
        super(message);
    }

    public BookingNotCancellableException(String message, Throwable cause) {
        super(message, cause);
    }
}
