package de.jensgiehl.marcqr.qr;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Controller
public class QrController {

    private final QrCodeService qrCodeService;
    private final BatchQrService batchQrService;

    public QrController(QrCodeService qrCodeService, BatchQrService batchQrService) {
        this.qrCodeService = qrCodeService;
        this.batchQrService = batchQrService;
    }

    @GetMapping("/qr")
    public String qrPage(Model model) {
        model.addAttribute("activePage", "qr");
        return "qr";
    }

    @GetMapping("/qr/batch")
    public String batchPage(Model model) {
        model.addAttribute("activePage", "batch");
        return "qr-batch";
    }

    @PostMapping(path = "/api/qr", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> generateQr(
            @RequestParam String content,
            @RequestParam(defaultValue = "360") int size,
            @RequestParam(defaultValue = "#132238") String foreground,
            @RequestParam(defaultValue = "#ffffff") String background,
            @RequestParam(defaultValue = "0") int cornerRadius,
            @RequestParam(defaultValue = "4") int imageCornerRadius,
            @RequestParam(required = false) MultipartFile logo) throws IOException {
        byte[] png = qrCodeService.generate(content,
                new QrSettings(size, foreground, background, cornerRadius, imageCornerRadius), readLogo(logo));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.IMAGE_PNG)
                .body(png);
    }

    @PostMapping(path = "/api/qr/batch", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<BatchQrItem> generateBatch(
            @RequestParam String lines,
            @RequestParam(defaultValue = "300") int size,
            @RequestParam(defaultValue = "#132238") String foreground,
            @RequestParam(defaultValue = "#ffffff") String background,
            @RequestParam(defaultValue = "0") int cornerRadius,
            @RequestParam(defaultValue = "4") int imageCornerRadius,
            @RequestParam(required = false) MultipartFile logo) throws IOException {
        return batchQrService.generate(lines,
                new QrSettings(size, foreground, background, cornerRadius, imageCornerRadius), readLogo(logo));
    }

    private byte[] readLogo(MultipartFile logo) throws IOException {
        return logo == null || logo.isEmpty() ? null : logo.getBytes();
    }
}
