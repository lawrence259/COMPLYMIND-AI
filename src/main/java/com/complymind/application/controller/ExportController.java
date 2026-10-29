package com.complymind.application.controller;

import com.complymind.application.entity.ComplianceApplication;
import com.complymind.application.service.ComplianceApplicationService;
import com.complymind.correctiveaction.entity.CorrectiveAction;
import com.complymind.correctiveaction.service.CorrectiveActionService;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Handles CSV and Excel export of compliance data.
 * All data comes from live database queries — no hardcoded or fabricated values.
 */
@Controller
@RequestMapping("/export")
public class ExportController {

    private final ComplianceApplicationService applicationService;
    private final CorrectiveActionService correctiveActionService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public ExportController(ComplianceApplicationService applicationService,
                            CorrectiveActionService correctiveActionService) {
        this.applicationService = applicationService;
        this.correctiveActionService = correctiveActionService;
    }

    // ─────────────────────────────────────────────
    // CSV Export: All Applications
    // ─────────────────────────────────────────────
    @GetMapping("/applications.csv")
    public void exportApplicationsCsv(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"applications-export.csv\"");

        List<ComplianceApplication> apps = applicationService.getAllApplications();

        PrintWriter writer = response.getWriter();
        // BOM for Excel UTF-8 compatibility
        writer.write('\uFEFF');
        writer.println("Application Number,Title,Institution,Framework,Academic Year,Status,Risk Score,Risk Band,Created At,Submitted At");

        for (ComplianceApplication app : apps) {
            writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%s,\"%s\",\"%s\",\"%s\"%n",
                    esc(app.getApplicationNumber()),
                    esc(app.getTitle()),
                    esc(app.getInstitution() != null ? app.getInstitution().getName() : ""),
                    esc(app.getFramework() != null ? app.getFramework().getCode() : ""),
                    esc(app.getAcademicYear()),
                    esc(app.getStatus() != null ? app.getStatus().name() : ""),
                    app.getRiskScore() != null ? app.getRiskScore().toPlainString() : "0",
                    esc(app.getRiskBand() != null ? app.getRiskBand().name() : ""),
                    app.getCreatedAt() != null ? app.getCreatedAt().format(DATE_FMT) : "",
                    app.getSubmittedAt() != null ? app.getSubmittedAt().format(DATE_FMT) : ""
            );
        }
        writer.flush();
    }

    // ─────────────────────────────────────────────
    // Excel Export: All Applications (.xlsx)
    // ─────────────────────────────────────────────
    @GetMapping("/applications.xlsx")
    public void exportApplicationsExcel(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"applications-export.xlsx\"");

        List<ComplianceApplication> apps = applicationService.getAllApplications();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Applications");

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font whiteFont = workbook.createFont();
            whiteFont.setColor(IndexedColors.WHITE.getIndex());
            whiteFont.setBold(true);
            headerStyle.setFont(whiteFont);

            // Header row
            String[] headers = {"App Number", "Title", "Institution", "Framework", "Academic Year",
                    "Status", "Risk Score", "Risk Band", "Created At", "Submitted At"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowNum = 1;
            for (ComplianceApplication app : apps) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(nvl(app.getApplicationNumber()));
                row.createCell(1).setCellValue(nvl(app.getTitle()));
                row.createCell(2).setCellValue(app.getInstitution() != null ? app.getInstitution().getName() : "");
                row.createCell(3).setCellValue(app.getFramework() != null ? app.getFramework().getCode() : "");
                row.createCell(4).setCellValue(nvl(app.getAcademicYear()));
                row.createCell(5).setCellValue(app.getStatus() != null ? app.getStatus().name() : "");
                row.createCell(6).setCellValue(app.getRiskScore() != null ? app.getRiskScore().doubleValue() : 0.0);
                row.createCell(7).setCellValue(app.getRiskBand() != null ? app.getRiskBand().name() : "");
                row.createCell(8).setCellValue(app.getCreatedAt() != null ? app.getCreatedAt().format(DATE_FMT) : "");
                row.createCell(9).setCellValue(app.getSubmittedAt() != null ? app.getSubmittedAt().format(DATE_FMT) : "");
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(response.getOutputStream());
        }
    }

    // ─────────────────────────────────────────────
    // CSV Export: Corrective Actions
    // ─────────────────────────────────────────────
    @GetMapping("/corrective-actions.csv")
    public void exportCorrectiveActionsCsv(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"corrective-actions-export.csv\"");

        List<CorrectiveAction> actions = correctiveActionService.getAllActions();

        PrintWriter writer = response.getWriter();
        writer.write('\uFEFF');
        writer.println("Title,Application,Root Issue,Assigned To,Due Date,Priority,Status,Resolution Summary");

        for (CorrectiveAction act : actions) {
            writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                    esc(act.getTitle()),
                    esc(act.getApplication() != null ? act.getApplication().getApplicationNumber() : ""),
                    esc(act.getRootIssue()),
                    esc(act.getAssignedTo() != null ? act.getAssignedTo().getFullName() : "Unassigned"),
                    act.getDueDate() != null ? act.getDueDate().format(DATE_FMT) : "",
                    esc(act.getPriority() != null ? act.getPriority().name() : ""),
                    esc(act.getStatus() != null ? act.getStatus().name() : ""),
                    esc(act.getResolutionSummary())
            );
        }
        writer.flush();
    }

    // ─────────────────────────────────────────────
    // CSV Export: Single Application Risk Report
    // ─────────────────────────────────────────────
    @GetMapping("/applications/{id}/report.csv")
    public void exportApplicationReportCsv(@PathVariable Long id, HttpServletResponse response) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"application-" + id + "-report.csv\"");

        ComplianceApplication app = applicationService.getApplicationById(id);

        PrintWriter writer = response.getWriter();
        writer.write('\uFEFF');
        writer.println("COMPLYMIND AI — Application Report");
        writer.println("Generated At," + java.time.LocalDateTime.now().format(DATE_FMT));
        writer.println();
        writer.println("Application Number," + esc(app.getApplicationNumber()));
        writer.println("Title," + esc(app.getTitle()));
        writer.println("Institution," + esc(app.getInstitution() != null ? app.getInstitution().getName() : ""));
        writer.println("Framework," + esc(app.getFramework() != null ? app.getFramework().getCode() : ""));
        writer.println("Academic Year," + esc(app.getAcademicYear()));
        writer.println("Status," + esc(app.getStatus() != null ? app.getStatus().name() : ""));
        writer.println("Risk Score," + (app.getRiskScore() != null ? app.getRiskScore().toPlainString() : "0"));
        writer.println("Risk Band," + esc(app.getRiskBand() != null ? app.getRiskBand().name() : ""));
        writer.println("Created At," + (app.getCreatedAt() != null ? app.getCreatedAt().format(DATE_FMT) : ""));
        writer.println("Submitted At," + (app.getSubmittedAt() != null ? app.getSubmittedAt().format(DATE_FMT) : "Not submitted"));
        writer.flush();
    }

    // ─── Helpers ───────────────────────────────
    private String esc(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }

    private String nvl(String value) {
        return value != null ? value : "";
    }
}
