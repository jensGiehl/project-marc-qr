package de.jensgiehl.marcqr.qr;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class BatchQrService {

    public static final int MAX_LINES = 100;
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern URL = Pattern.compile(
            "^(?:(?:https?://)|(?:www\\.))?([a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,}(?::\\d+)?(?:[/?#].*)?$",
            Pattern.CASE_INSENSITIVE);

    private final QrCodeService qrCodeService;
    private final QrFilenameService qrFilenameService;

    public BatchQrService(QrCodeService qrCodeService, QrFilenameService qrFilenameService) {
        this.qrCodeService = qrCodeService;
        this.qrFilenameService = qrFilenameService;
    }

    public List<BatchQrItem> generate(String lines, QrSettings settings, byte[] logoBytes) {
        if (lines == null) {
            throw new QrValidationException("Bitte mindestens eine Zeile eingeben.");
        }
        List<String> inputs = lines.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();
        if (inputs.isEmpty()) {
            throw new QrValidationException("Bitte mindestens eine nicht leere Zeile eingeben.");
        }
        if (inputs.size() > MAX_LINES) {
            throw new QrValidationException("Pro Durchlauf sind höchstens %d QR-Codes möglich."
                    .formatted(MAX_LINES));
        }

        List<BatchQrItem> result = new ArrayList<>(inputs.size());
        for (String input : inputs) {
            DetectedContent detected = detect(input);
            byte[] png = qrCodeService.generate(detected.payload(), settings, logoBytes);
            result.add(new BatchQrItem(input, detected.type(), detected.payload(),
                    Base64.getEncoder().encodeToString(png), qrFilenameService.create(input)));
        }
        return result;
    }

    private DetectedContent detect(String input) {
        if (EMAIL.matcher(input).matches()) {
            return new DetectedContent("E-Mail", "mailto:" + input);
        }
        if (URL.matcher(input).matches()) {
            String payload = input.matches("(?i)^https?://.*") ? input : "https://" + input;
            return new DetectedContent("Webseite", payload);
        }
        return new DetectedContent("Text", input);
    }

    private record DetectedContent(String type, String payload) {
    }
}
