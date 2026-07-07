package com.epiis.projectcasaketteler.helper;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class DocumentValidationHelper {

    private static final long MAX_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB
    private static final String[] ALLOWED_EXTENSIONS = { "pdf", "jpg", "jpeg", "png" };

    public String validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "El archivo está vacío o no fue enviado";
        }

        // Validar tamaño
        if (file.getSize() > MAX_SIZE_BYTES) {
            return "El archivo supera el límite de 10 MB";
        }

        // Validar extensión
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || !originalFileName.contains(".")) {
            return "El archivo no tiene una extensión válida";
        }

        String extension = originalFileName
                .substring(originalFileName.lastIndexOf(".") + 1)
                .toLowerCase();

        for (String allowed : ALLOWED_EXTENSIONS) {
            if (allowed.equals(extension)) {
                return null; // null = sin error, archivo válido
            }
        }

        return "Tipo de archivo no permitido. Solo se aceptan PDF, JPG y PNG";
    }
}