package de.jensgiehl.marcqr.code;

import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

@Service
public class ExcelExportService {

    public void write(List<String> codes, OutputStream outputStream) throws IOException {
        try (var workbook = new SXSSFWorkbook(200)) {
            workbook.setCompressTempFiles(true);
            var sheet = workbook.createSheet("Codes");
            sheet.trackAllColumnsForAutoSizing();
            sheet.createFreezePane(0, 1);

            var headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            var headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            var header = sheet.createRow(0);
            var codeHeader = header.createCell(0);
            codeHeader.setCellValue("Code");
            codeHeader.setCellStyle(headerStyle);
            var descriptionHeader = header.createCell(1);
            descriptionHeader.setCellValue("Beschreibung");
            descriptionHeader.setCellStyle(headerStyle);

            for (int index = 0; index < codes.size(); index++) {
                var row = sheet.createRow(index + 1);
                row.createCell(0).setCellValue(codes.get(index));
                row.createCell(1).setCellValue("");
            }

            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);
            sheet.setColumnWidth(0, Math.max(sheet.getColumnWidth(0), 14 * 256));
            sheet.setColumnWidth(1, Math.max(sheet.getColumnWidth(1), 24 * 256));
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, codes.size(), 0, 1));
            workbook.write(outputStream);
        }
    }
}
