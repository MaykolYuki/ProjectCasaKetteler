package com.epiis.projectcasaketteler.helper;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.springframework.stereotype.Component;

import com.epiis.projectcasaketteler.dto.response.ResponseFaceVerification;

import tools.jackson.databind.ObjectMapper;

@Component
public class PythonFaceRecognitionHelper {

    // ---------------------------------------------------------------------------------
    // ACTUALIZADO: Ahora apunta al ejecutable de Python dentro de venv_oficial
    // nativo
    // ---------------------------------------------------------------------------------
    private static final String PYTHON_PATH = "C:/Users/yerry/Documents/Ingenieria de Software/BackendCasaKetteler/ProjectCasaKetteler/python_scripts/venv_perfecto/Scripts/python.exe";

    private static final String SCRIPT_PATH = "python_scripts/ReconocimientoFacial.py";

    public ResponseFaceVerification verificarRostro(String rutaImagen, String idUser) {
        try {
            // 1. Convertimos las rutas a archivos físicos reales de Java para sacar su ruta
            // absoluta
            java.io.File archivoCaptura = new java.io.File(rutaImagen);
            java.io.File archivoDirectorioUsuario = new java.io.File("storage/Photo/" + idUser);

            String rutaAbsolutaCaptura = archivoCaptura.getAbsolutePath();
            String rutaAbsolutaUsuario = archivoDirectorioUsuario.getAbsolutePath();

            // 2. Le pasamos las rutas absolutas completas a Python (ej:
            // C:/Users/yerry/.../storage/captura.jpg)
            ProcessBuilder processBuilder = new ProcessBuilder(
                    PYTHON_PATH,
                    SCRIPT_PATH,
                    rutaAbsolutaCaptura,
                    rutaAbsolutaUsuario);

            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

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