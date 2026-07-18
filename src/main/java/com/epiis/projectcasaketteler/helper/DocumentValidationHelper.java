package com.epiis.projectcasaketteler.helper;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class DocumentValidationHelper {

    private static final long MAX_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB
    private static final String[] ALLOWED_EXTENSIONS = { "pdf", "jpg", "jpeg", "png" };
    private static final String[] ALLOWED_IMAGE_EXTENSIONS = { "jpg", "jpeg", "png" };

    public String validate(MultipartFile file) {
        return validate(file, ALLOWED_EXTENSIONS);
    }

    /** Para fotos de rostro: solo imágenes, nunca PDF. */
    public String validateImage(MultipartFile file) {
        return validate(file, ALLOWED_IMAGE_EXTENSIONS);
    }

    private String validate(MultipartFile file, String[] extensionesPermitidas) {
        if (file == null || file.isEmpty()) {
            return "El archivo está vacío o no fue enviado";
        }

        if (file.getSize() > MAX_SIZE_BYTES) {
            return "El archivo supera el límite de 10 MB";
        }

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || !originalFileName.contains(".")) {
            return "El archivo no tiene una extensión válida";
        }

        String extension = originalFileName
                .substring(originalFileName.lastIndexOf(".") + 1)
                .toLowerCase();

        boolean extensionPermitida = false;
        for (String allowed : extensionesPermitidas) {
            if (allowed.equals(extension)) {
                extensionPermitida = true;
                break;
            }
        }
        if (!extensionPermitida) {
            return "Tipo de archivo no permitido para este campo";
        }

        String tipoReal = detectarTipoReal(file);
        if (tipoReal == null) {
            return "No se pudo verificar el contenido del archivo. Asegúrate de que sea un archivo válido";
        }

        boolean esJpgYAfirmaJpg = tipoReal.equals("jpg") && (extension.equals("jpg") || extension.equals("jpeg"));
        boolean coincideDirecto = tipoReal.equals(extension);

        if (!coincideDirecto && !esJpgYAfirmaJpg) {
            return "El contenido del archivo no coincide con su extensión (." + extension + ")";
        }

        return null; // sin error, archivo válido
    }

    /**
     * Detecta el tipo real del archivo leyendo su firma binaria (magic bytes),
     * ignorando por completo el nombre/extensión que declara el cliente.
     */
    private String detectarTipoReal(MultipartFile file) {
        byte[] header = new byte[8];
        try (InputStream is = file.getInputStream()) {
            int leidos = is.read(header);
            if (leidos < 4) {
                return null;
            }
        } catch (IOException e) {
            return null;
        }

        // PDF: "%PDF"
        if (header[0] == 0x25 && header[1] == 0x50 && header[2] == 0x44 && header[3] == 0x46) {
            return "pdf";
        }

        // JPEG: FF D8 FF
        if ((header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) {
            return "jpg";
        }

        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if ((header[0] & 0xFF) == 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47) {
            return "png";
        }

        return null;
    }
}