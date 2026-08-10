package de.jensgiehl.marcqr.code;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
public class CodeController {

    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private final CodeGeneratorService codeGeneratorService;
    private final ExcelExportService excelExportService;

    public CodeController(CodeGeneratorService codeGeneratorService, ExcelExportService excelExportService) {
        this.codeGeneratorService = codeGeneratorService;
        this.excelExportService = excelExportService;
    }

    @GetMapping("/codes")
    public String codesPage(Model model) {
        model.addAttribute("activePage", "codes");
        return "codes";
    }

    @PostMapping("/codes/excel")
    public void generateExcel(
            @RequestParam(name = "characters") List<String> characters,
            @RequestParam(defaultValue = "4") int length,
            @RequestParam(defaultValue = "1") int maxDigits,
            @RequestParam(defaultValue = "100") int count,
            @RequestParam(required = false) String downloadToken,
            HttpServletResponse response) throws IOException {
        var request = new CodeGenerationRequest(characters, length, maxDigits, count);
        List<String> codes = codeGeneratorService.generate(request);

        String filename = "codes-%s.xlsx".formatted(FILE_TIME.format(LocalDateTime.now()));
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        response.setHeader("Cache-Control", "no-store");
        if (downloadToken != null && !downloadToken.isBlank()) {
            String safeToken = URLEncoder.encode(downloadToken, StandardCharsets.UTF_8);
            response.addHeader("Set-Cookie", "downloadToken=" + safeToken
                    + "; Max-Age=60; Path=/; SameSite=Lax");
        }
        excelExportService.write(codes, response.getOutputStream());
    }
}
