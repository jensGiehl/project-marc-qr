package de.jensgiehl.marcqr.qr;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;
import java.util.Map;

@RestControllerAdvice(assignableTypes = QrController.class)
public class QrApiExceptionHandler {

    @ExceptionHandler(QrValidationException.class)
    public ResponseEntity<Map<String, String>> handleValidation(QrValidationException exception) {
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<Map<String, String>> handleImageRead(IOException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("message", "Die hochgeladene Datei konnte nicht gelesen werden."));
    }
}
