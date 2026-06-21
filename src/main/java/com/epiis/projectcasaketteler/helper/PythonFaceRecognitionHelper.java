package com.epiis.projectcasaketteler.helper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

import org.springframework.stereotype.Component;

import com.epiis.projectcasaketteler.dto.response.ResponseFaceVerification;
import com.epiis.projectcasaketteler.dto.response.ResponsePhotoFilter;

import tools.jackson.databind.ObjectMapper;

@Component
public class PythonFaceRecognitionHelper {

    private static final String PYTHON_PATH = "C:/Users/yerry/Documents/Ingenieria de Software/BackendCasaKetteler/ProjectCasaKetteler/python_scripts/venv_perfecto/Scripts/python.exe";

    private static final String SCRIPT_PATH = "python_scripts/ReconocimientoFacial.py";

    private static final String FILTER_SCRIPT_PATH = "python_scripts/FiltroCalidadFoto.py";

    public ResponseFaceVerification verificarRostro(String rutaImagen, String idUser, String bestPhotoFileName) {
        try {
            java.io.File archivoCaptura = new java.io.File(rutaImagen);
            String rutaAbsolutaCaptura = archivoCaptura.getAbsolutePath();

            String directorioUsuario = "storage/Photo/" + idUser;

            // Si ya hay una referencia cacheada, úsala directamente
            String mejorFotoNombre;
            if (bestPhotoFileName != null && !bestPhotoFileName.isEmpty()) {
                mejorFotoNombre = bestPhotoFileName;
            } else {
                // Fallback: calcular si no hay caché (primera vez o caché perdida)
                ResponsePhotoFilter filtro = seleccionarMejorFoto(directorioUsuario);
                if (!filtro.isSuccess()) {
                    ResponseFaceVerification error = new ResponseFaceVerification();
                    error.setVerified(false);
                    error.setError(filtro.getError());
                    return error;
                }
                mejorFotoNombre = filtro.getBestImage();
            }

            java.io.File mejorFoto = new java.io.File(directorioUsuario + "/" + mejorFotoNombre);
            String rutaAbsolutaMejorFoto = mejorFoto.getAbsolutePath();

            ProcessBuilder processBuilder = new ProcessBuilder(
                    PYTHON_PATH,
                    SCRIPT_PATH,
                    rutaAbsolutaCaptura,
                    rutaAbsolutaMejorFoto);

            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            StringBuilder jsonOutput = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().startsWith("{") && line.trim().endsWith("}")) {
                    jsonOutput.append(line.trim());
                }
            }

            process.waitFor();

            if (jsonOutput.length() == 0) {
                throw new RuntimeException("Python no devolvió un JSON válido.");
            }

            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(jsonOutput.toString(), ResponseFaceVerification.class);

        } catch (Exception e) {
            ResponseFaceVerification error = new ResponseFaceVerification();
            error.setVerified(false);
            error.setError(e.getMessage());
            return error;
        }
    }

    public ResponseFaceVerification verificarRostroBase64(String base64Image, String idUser, String bestPhotoFileName) {
        File tempFile = null;
        try {
            byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Image);

            String tempDir = "temp/";
            new File(tempDir).mkdirs();

            String fileName = "sync_" + idUser + "_" + System.currentTimeMillis() + ".jpg";
            tempFile = new File(tempDir + fileName);

            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(imageBytes);
            }

            return verificarRostro(tempFile.getAbsolutePath(), idUser, bestPhotoFileName);

        } catch (Exception e) {
            ResponseFaceVerification error = new ResponseFaceVerification();
            error.setVerified(false);
            error.setError("Error decodificando imagen: " + e.getMessage());
            return error;
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    public ResponsePhotoFilter seleccionarMejorFoto(String directorioUsuario) {
        try {
            java.io.File directorio = new java.io.File(directorioUsuario);
            String rutaAbsoluta = directorio.getAbsolutePath();

            ProcessBuilder processBuilder = new ProcessBuilder(
                    PYTHON_PATH,
                    FILTER_SCRIPT_PATH,
                    rutaAbsoluta);

            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            StringBuilder jsonOutput = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().startsWith("{") && line.trim().endsWith("}")) {
                    jsonOutput.append(line.trim());
                }
            }

            process.waitFor();

            if (jsonOutput.length() == 0) {
                throw new RuntimeException("FiltroCalidadFoto no devolvió JSON válido.");
            }

            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(jsonOutput.toString(), ResponsePhotoFilter.class);

        } catch (Exception e) {
            ResponsePhotoFilter error = new ResponsePhotoFilter();
            error.setSuccess(false);
            error.setError("Error en filtro de calidad: " + e.getMessage());
            return error;
        }
    }

    /**
     * // -----------------------------------------------------------------
     * // MÉTODO DE DIAGNÓSTICO DE RUTAS NATIVAS - ÚTIL PARA DESARROLLO
     * // -----------------------------------------------------------------
     * public static void main(String[] args) {
     * System.out.println("=== DIAGNÓSTICO DE RUTAS NATIVAS ===");
     * 
     * // 1. Descubrir dónde está parada la app en este momento
     * String rutaRaizJava = new java.io.File(".").getAbsolutePath();
     * System.out.println("📍 Tu proyecto Java se está ejecutando en: " +
     * rutaRaizJava);
     * 
     * // 2. Comprobar si el script de Python está en el lugar correcto
     * java.io.File scriptPython = new java.io.File(SCRIPT_PATH);
     * System.out.println("📂 ¿Se encuentra el script de Python en '" + SCRIPT_PATH
     * + "'?: " + scriptPython.exists());
     * if (scriptPython.exists()) {
     * System.out.println(" -> Ruta absoluta real del script: " +
     * scriptPython.getAbsolutePath());
     * }
     * 
     * // 3. Comprobar la carpeta storage
     * java.io.File carpetaStorage = new java.io.File("storage");
     * System.out.println("📁 ¿Existe la carpeta 'storage' en la raíz?: " +
     * carpetaStorage.exists());
     * if (carpetaStorage.exists()) {
     * System.out.println(" -> Ruta absoluta real de storage: " +
     * carpetaStorage.getAbsolutePath());
     * } else {
     * System.out
     * .println(" ⚠️ ATENCIÓN: Debes crear la carpeta 'storage' manualmente dentro
     * de: " + rutaRaizJava);
     * }
     * 
     * System.out.println("\n------------------------------------------------");
     * System.out.println("Ejecutando script de prueba...");
     * System.out.println("------------------------------------------------");
     * 
     * PythonFaceRecognitionHelper helper = new PythonFaceRecognitionHelper();
     * String rutaImagenCapturada = "storage/captura.jpeg";
     * String idUsuarioPrueba = "999";
     * 
     * // Ejecutamos la lógica
     * ResponseFaceVerification respuesta =
     * helper.verificarRostro(rutaImagenCapturada, idUsuarioPrueba);
     * 
     * System.out.println("=== RESPUESTA RECIBIDA DESDE PYTHON ===");
     * System.out.println("¿Es el mismo usuario? (Verified): " +
     * respuesta.isVerified()); // O .isVerified() según tus
     * // getters
     * 
     * if (respuesta.getError() != null) {
     * System.out.println("⚠️ ERROR DETECTADO POR PYTHON: " + respuesta.getError());
     * } else {
     * System.out.println("¡Conexión perfecta! El JSON se transformó correctamente
     * en Java.");
     * }
     * System.out.println("================================================");
     * }
     */
}