package de.jensgiehl.marcqr.qr;

public class QrValidationException extends RuntimeException {

    public QrValidationException(String message) {
        super(message);
    }

    public QrValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
