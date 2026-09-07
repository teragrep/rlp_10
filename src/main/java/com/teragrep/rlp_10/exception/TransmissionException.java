package com.teragrep.rlp_10.exception;

public class TransmissionException extends RuntimeException {
    public TransmissionException(String message) {
        super(message);
    }
    public TransmissionException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public TransmissionException(final Throwable cause) {
        super(cause);
    }
}
