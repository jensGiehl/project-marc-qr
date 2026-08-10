package de.jensgiehl.marcqr.code;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExcelExportServiceTest {

    @Test
    void createsFormattedWorkbookWithFrozenHeader() throws Exception {
        var output = new ByteArrayOutputStream();
        new ExcelExportService().write(List.of("AB2C", "XYZ3"), output);

        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(output.toByteArray()))) {
            var sheet = workbook.getSheet("Codes");
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Code");
            assertThat(sheet.getRow(0).getCell(1).getStringCellValue()).isEqualTo("Beschreibung");
            assertThat(sheet.getRow(0).getCell(0).getCellStyle().getFontIndexAsInt()).isPositive();
            assertThat(sheet.getPaneInformation().isFreezePane()).isTrue();
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("AB2C");
            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEmpty();
        }
    }
}
