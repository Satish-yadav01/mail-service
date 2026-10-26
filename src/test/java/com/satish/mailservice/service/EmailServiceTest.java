package com.satish.mailservice.service;

import com.satish.mailservice.entity.Email;
import com.satish.mailservice.repository.EmailRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class EmailServiceTest {

    @Test
    void importsEvaluatedValuesFromFormulaCells() throws IOException {
        byte[] workbookBytes;
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet();
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Email");
            header.createCell(1).setCellValue("First name");
            header.createCell(2).setCellValue("Last name");

            Row data = sheet.createRow(1);
            data.createCell(0).setCellValue("akanksha@example.com");
            data.createCell(3).setCellValue("Akanksha Puri");
            data.createCell(1).setCellFormula("IFERROR(LEFT(D2,SEARCH(\" \",D2)-1),\"\")");
            data.createCell(2).setCellFormula("IFERROR(RIGHT(D2,LEN(D2)-SEARCH(\" \",D2)),\"\")");
            workbook.write(output);
            workbookBytes = output.toByteArray();
        }

        EmailRepository emailRepository = mock(EmailRepository.class);
        when(emailRepository.existsByEmail("akanksha@example.com")).thenReturn(false);
        EmailService emailService = new EmailService(emailRepository);

        emailService.importEmailsFromExcel(workbookBytes, "emails.xlsx");

        ArgumentCaptor<Email> emailCaptor = ArgumentCaptor.forClass(Email.class);
        verify(emailRepository).save(emailCaptor.capture());
        assertEquals("Akanksha", emailCaptor.getValue().getFirstName());
        assertEquals("Puri", emailCaptor.getValue().getLastName());
    }
}
