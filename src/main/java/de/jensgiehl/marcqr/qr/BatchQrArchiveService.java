package de.jensgiehl.marcqr.qr;

import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class BatchQrArchiveService {

    public byte[] create(Iterable<BatchQrItem> items) throws IOException {
        ByteArrayOutputStream archive = new ByteArrayOutputStream();
        Set<String> filenames = new HashSet<>();
        try (ZipOutputStream zip = new ZipOutputStream(archive)) {
            for (BatchQrItem item : items) {
                String filename = uniqueFilename(item.filename(), filenames);
                zip.putNextEntry(new ZipEntry(filename));
                zip.write(Base64.getDecoder().decode(item.imageBase64()));
                zip.closeEntry();
            }
        }
        return archive.toByteArray();
    }

    private String uniqueFilename(String filename, Set<String> filenames) {
        if (filenames.add(filename.toLowerCase(Locale.ROOT))) {
            return filename;
        }

        String basename = filename.substring(0, filename.length() - ".png".length());
        int suffix = 2;
        String candidate;
        do {
            candidate = "%s-%d.png".formatted(basename, suffix++);
        } while (!filenames.add(candidate.toLowerCase(Locale.ROOT)));
        return candidate;
    }
}
