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

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import com.epiis.projectcasaketteler.entity.EntityAttendance;
import com.epiis.projectcasaketteler.entity.EntityUser;
import com.epiis.projectcasaketteler.repository.RepositoryAttendance;
import com.epiis.projectcasaketteler.repository.RepositoryUser;

@Service
public class BusinessAttendanceExport {

    @Autowired
    private RepositoryAttendance repositoryAttendance;

    @Autowired
    private RepositoryUser repositoryUser;

    private List<EntityAttendance> obtenerDatos(String idUser, String fechaInicio,
            String fechaFin, Boolean estado) {
        Date inicio = parseFecha(fechaInicio, false);
        Date fin = parseFecha(fechaFin, true);

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0,
                Integer.MAX_VALUE);

        if (idUser != null && !idUser.isEmpty()) {
            Optional<EntityUser> optional = repositoryUser.findById(idUser);
            if (optional.isPresent()) {
                return repositoryAttendance.findByFilters(
                        optional.get(), inicio, fin, estado, pageable).getContent();
            }
            return List.of();
        }

        return repositoryAttendance.findByFiltersAdmin(
                inicio, fin, estado, null, pageable).getContent();
    }

    public byte[] exportarExcel(String idUser, String fechaInicio,
            String fechaFin, Boolean estado) throws Exception {
        List<EntityAttendance> registros = obtenerDatos(idUser, fechaInicio, fechaFin, estado);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Asistencias");

            // Estilo encabezado
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Encabezados
            Row header = sheet.createRow(0);
            String[] columnas = { "ID", "Residente", "Fecha Entrada", "Fecha Salida", "Estado", "Duración (min)" };
            for (int i = 0; i < columnas.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columnas[i]);
                cell.setCellStyle(headerStyle);
            }

            // Datos
            int rowNum = 1;
            for (EntityAttendance a : registros) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(a.getIdAtendance());
                row.createCell(1).setCellValue(
                        a.getParentUser() != null
                                ? a.getParentUser().getFirstName() + " " + a.getParentUser().getSurName()
                                : "");
                row.createCell(2).setCellValue(
                        a.getEntryDate() != null ? a.getEntryDate().toString() : "");
                row.createCell(3).setCellValue(
                        a.getDepartureDate() != null ? a.getDepartureDate().toString() : "Abierta");
                row.createCell(4).setCellValue(Boolean.TRUE.equals(a.getStatus()) ? "Entrada" : "Salida");

                // Duración en minutos
                if (a.getEntryDate() != null && a.getDepartureDate() != null) {
                    long diff = (a.getDepartureDate().getTime() - a.getEntryDate().getTime()) / (1000 * 60);
                    row.createCell(5).setCellValue(diff);
                } else {
                    row.createCell(5).setCellValue("-");
                }
            }

            // Autoajustar columnas
            for (int i = 0; i < columnas.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] exportarPDF(String idUser, String fechaInicio,
            String fechaFin, Boolean estado) throws Exception {
        List<EntityAttendance> registros = obtenerDatos(idUser, fechaInicio, fechaFin, estado);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, out);
        document.open();

        // Fuentes
        com.lowagie.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
        com.lowagie.text.Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
        com.lowagie.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        com.lowagie.text.Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 9);

        // Título
        Paragraph title = new Paragraph("Reporte de Asistencias", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        // Fecha de generación
        Paragraph fecha = new Paragraph("Generado: " + new Date().toString(), subFont);
        fecha.setAlignment(Element.ALIGN_RIGHT);
        fecha.setSpacingAfter(10);
        document.add(fecha);

        // Tabla
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 3f, 4f, 4f, 4f, 2f, 2f });

        // Encabezados
        String[] columnas = { "ID", "Residente", "Entrada", "Salida", "Estado", "Dur. (min)" };
        for (String col : columnas) {
            PdfPCell cell = new PdfPCell(new Phrase(col, headerFont));
            cell.setBackgroundColor(new java.awt.Color(173, 216, 230));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(5);
            table.addCell(cell);
        }

        // Datos
        for (EntityAttendance a : registros) {
            table.addCell(new Phrase(a.getIdAtendance().substring(0, 8) + "...", dataFont));
            table.addCell(new Phrase(
                    a.getParentUser() != null ? a.getParentUser().getFirstName() + " " + a.getParentUser().getSurName()
                            : "",
                    dataFont));
            table.addCell(new Phrase(
                    a.getEntryDate() != null ? a.getEntryDate().toString() : "", dataFont));
            table.addCell(new Phrase(
                    a.getDepartureDate() != null ? a.getDepartureDate().toString() : "Abierta", dataFont));
            table.addCell(new Phrase(
                    Boolean.TRUE.equals(a.getStatus()) ? "Entrada" : "Salida", dataFont));

            if (a.getEntryDate() != null && a.getDepartureDate() != null) {
                long diff = (a.getDepartureDate().getTime() - a.getEntryDate().getTime()) / (1000 * 60);
                table.addCell(new Phrase(String.valueOf(diff), dataFont));
            } else {
                table.addCell(new Phrase("-", dataFont));
            }
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