package de.jensgiehl.marcqr.code;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record CodeGenerationRequest(List<String> characters, int length, int maxDigits, int count) {

    public static final int MAX_LENGTH = 32;
    public static final int MAX_EXPORT_COUNT = 100_000;
    private static final String ALLOWED = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public CodeGenerationRequest {
        if (characters == null) {
            characters = List.of();
        }
        Set<String> unique = new LinkedHashSet<>(characters);
        if (unique.isEmpty()) {
            throw new CodeValidationException("Bitte mindestens ein Zeichen auswählen.");
        }
        if (unique.stream().anyMatch(value -> value == null || value.length() != 1
                || ALLOWED.indexOf(value.charAt(0)) < 0)) {
            throw new CodeValidationException("Die Zeichenauswahl enthält einen ungültigen Wert.");
        }
        characters = List.copyOf(unique);
        if (length < 1 || length > MAX_LENGTH) {
            throw new CodeValidationException("Die Codelänge muss zwischen 1 und %d liegen."
                    .formatted(MAX_LENGTH));
        }
        if (maxDigits < 0 || maxDigits > length) {
            throw new CodeValidationException("Die maximale Ziffernanzahl muss zwischen 0 und der Codelänge liegen.");
        }
        if (count < 1 || count > MAX_EXPORT_COUNT) {
            throw new CodeValidationException("Die Anzahl muss zwischen 1 und %,d liegen."
                    .formatted(MAX_EXPORT_COUNT));
        }
    }
}
