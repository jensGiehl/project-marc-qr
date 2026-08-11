package de.jensgiehl.marcqr.qr;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class QrFilenameService {

    private static final int MAX_BASENAME_LENGTH = 120;
    private static final Pattern SCHEME = Pattern.compile("^[a-z][a-z0-9+.-]*://", Pattern.CASE_INSENSITIVE);
    private static final Set<String> RESERVED_NAMES = Set.of(
            "con", "prn", "aux", "nul",
            "com1", "com2", "com3", "com4", "com5", "com6", "com7", "com8", "com9",
            "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9");

    public String create(String content) {
        String normalized = Normalizer.normalize(content == null ? "" : content.trim(), Normalizer.Form.NFKC);
        String withoutScheme = SCHEME.matcher(normalized).replaceFirst("");
        StringBuilder basename = new StringBuilder();
        withoutScheme.codePoints()
                .filter(Character::isLetterOrDigit)
                .limit(MAX_BASENAME_LENGTH)
                .forEach(basename::appendCodePoint);

        String result = basename.isEmpty() ? "qr-code" : basename.toString();
        if (RESERVED_NAMES.contains(result.toLowerCase(Locale.ROOT))) {
            result = "qr-" + result;
        }
        return result + ".png";
    }
}
