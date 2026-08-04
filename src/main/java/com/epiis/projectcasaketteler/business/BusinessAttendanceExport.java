package com.epiis.projectcasaketteler.business;

import java.io.ByteArrayOutputStream;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.epiis.projectcasaketteler.entity.EntityAdmin;
import com.epiis.projectcasaketteler.entity.EntityAttendance;
import com.epiis.projectcasaketteler.entity.EntityAttendance.AttendanceEventType;
import com.epiis.projectcasaketteler.entity.EntityUser;
import com.epiis.projectcasaketteler.repository.RepositoryAdmin;
import com.epiis.projectcasaketteler.repository.RepositoryAttendance;
import com.epiis.projectcasaketteler.repository.RepositoryUser;

@Service
public class BusinessAttendanceExport {

    @Autowired
    private RepositoryAttendance repositoryAttendance;

    @Autowired
    private RepositoryUser repositoryUser;

    @Autowired
    private RepositoryAdmin repositoryAdmin;

    private String resolveResidenceScope(String adminId, String requestedIdResidence) {
        Optional<EntityAdmin> adminOpt = repositoryAdmin.findById(adminId);
        if (!adminOpt.isPresent()) {
            return requestedIdResidence;
        }
        EntityAdmin admin = adminOpt.get();
        if (admin.getRole() == EntityAdmin.AdminRole.SUPER_ADMIN) {
            return requestedIdResidence;
        }
        return admin.getParentResidence().getIdResidence();
    }

    private AttendanceEventType parseTipo(String tipo) {
        if (tipo == null || tipo.isEmpty())
            return null;
        try {
            return AttendanceEventType.valueOf(tipo);
        } catch (Exception e) {
            return null;
        }
    }

    private List<EntityAttendance> obtenerDatos(String adminId, String idUser, String fechaInicio,
            String fechaFin, String tipo) {
        Date inicio = parseFecha(fechaInicio, false);
        Date fin = parseFecha(fechaFin, true);
        AttendanceEventType tipoEvento = parseTipo(tipo);

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0,
                Integer.MAX_VALUE);

        if (idUser != null && !idUser.isEmpty()) {
            Optional<EntityUser> optional = repositoryUser.findById(idUser);
            if (optional.isPresent()) {
                return repositoryAttendance.findByFilters(
                        optional.get(), inicio, fin, tipoEvento, pageable).getContent();
            }
            return List.of();
        }

        String idResidence = resolveResidenceScope(adminId, null);
        return repositoryAttendance.findByFiltersAdmin(
                inicio, fin, tipoEvento, null, idResidence, pageable).getContent();
    }

    private String etiquetaTipo(EntityAttendance a) {
        if (a.getEventType() == null)
            return "";
        switch (a.getEventType()) {
            case ENTRADA:
                return "Entrada";
            case SALIDA:
                return "Salida";
            case INTENTO_FALLIDO:
                return "Intento fallido";
            default:
                return a.getEventType().name();
        }
    }

    @Transactional(readOnly = true)
    public byte[] exportarExcel(String adminId, String idUser, String fechaInicio,
            String fechaFin, String tipo) throws Exception {
        List<EntityAttendance> registros = obtenerDatos(adminId, idUser, fechaInicio, fechaFin, tipo);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Eventos de asistencia");

            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row header = sheet.createRow(0);
            String[] columnas = { "ID", "Residente", "Fecha y hora", "Tipo", "Anomalía", "Motivo fallo",
                    "Similitud (%)" };
            for (int i = 0; i < columnas.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columnas[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (EntityAttendance a : registros) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(a.getIdAtendance());
                row.createCell(1).setCellValue(
                        a.getParentUser() != null
                                ? a.getParentUser().getFirstName() + " " + a.getParentUser().getSurName()
                                : "");
                row.createCell(2).setCellValue(
                        a.getEventTimestamp() != null ? a.getEventTimestamp().toString() : "");
                row.createCell(3).setCellValue(etiquetaTipo(a));
                row.createCell(4).setCellValue(Boolean.TRUE.equals(a.getEsAnomalia()) ? "Sí" : "No");
                row.createCell(5).setCellValue(a.getMotivoFallo() != null ? a.getMotivoFallo() : "");
                row.createCell(6).setCellValue(
                        a.getServerSimilarity() != null ? String.format("%.1f", a.getServerSimilarity()) : "");
            }

            for (int i = 0; i < columnas.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    @Transactional(readOnly = true)
    public byte[] exportarPDF(String adminId, String idUser, String fechaInicio,
            String fechaFin, String tipo) throws Exception {
        List<EntityAttendance> registros = obtenerDatos(adminId, idUser, fechaInicio, fechaFin, tipo);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, out);
        document.open();

        com.lowagie.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
        com.lowagie.text.Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
        com.lowagie.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        com.lowagie.text.Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 9);

        Paragraph title = new Paragraph("Reporte de Eventos de Asistencia", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        Paragraph fecha = new Paragraph("Generado: " + new Date().toString(), subFont);
        fecha.setAlignment(Element.ALIGN_RIGHT);
        fecha.setSpacingAfter(10);
        document.add(fecha);

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 3f, 4f, 4f, 2.5f, 2f, 4f });

        String[] columnas = { "ID", "Residente", "Fecha y hora", "Tipo", "Anomalía", "Motivo fallo" };
        for (String col : columnas) {
            PdfPCell cell = new PdfPCell(new Phrase(col, headerFont));
            cell.setBackgroundColor(new java.awt.Color(173, 216, 230));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(5);
            table.addCell(cell);
        }

        for (EntityAttendance a : registros) {
            table.addCell(new Phrase(a.getIdAtendance().substring(0, 8) + "...", dataFont));
            table.addCell(new Phrase(
                    a.getParentUser() != null ? a.getParentUser().getFirstName() + " " + a.getParentUser().getSurName()
                            : "",
                    dataFont));
            table.addCell(new Phrase(
                    a.getEventTimestamp() != null ? a.getEventTimestamp().toString() : "", dataFont));
            table.addCell(new Phrase(etiquetaTipo(a), dataFont));
            table.addCell(new Phrase(Boolean.TRUE.equals(a.getEsAnomalia()) ? "Sí" : "No", dataFont));
            table.addCell(new Phrase(a.getMotivoFallo() != null ? a.getMotivoFallo() : "-", dataFont));
        }

        document.add(table);
        document.close();
        return out.toByteArray();
    }

    private Date parseFecha(String fecha, boolean finDelDia) {
        if (fecha == null || fecha.isEmpty())
            return null;
        try {
            java.time.LocalDate localDate = java.time.LocalDate.parse(fecha);
            if (finDelDia) {
                return java.sql.Date.valueOf(localDate.plusDays(1));
            }
            return java.sql.Date.valueOf(localDate);
        } catch (Exception e) {
            return null;
        }
    }
}