package com.epiis.projectcasaketteler.helper;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.epiis.projectcasaketteler.dto.response.ResponseFaceVerification;
import com.epiis.projectcasaketteler.dto.response.ResponsePhotoFilter;

import tools.jackson.databind.ObjectMapper;

@Component
public class PythonFaceRecognitionHelper {

    private static final String SERVER_URL = "http://localhost:5000";
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    private String postJson(String endpoint, Map<String, Object> body) throws Exception {
        String jsonBody = mapper.writeValueAsString(body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SERVER_URL + endpoint))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    public ResponsePhotoFilter seleccionarMejorFoto(String directorioUsuario) {
        try {
            File dir = new File(directorioUsuario);
            Map<String, Object> body = new HashMap<>();
            body.put("directorio", dir.getAbsolutePath());

            String json = postJson("/filtro", body);
            return mapper.readValue(json, ResponsePhotoFilter.class);

        } catch (Exception e) {
            ResponsePhotoFilter error = new ResponsePhotoFilter();
            error.setSuccess(false);
            error.setError("Error conectando al servidor de reconocimiento: " + e.getMessage());
            return error;
        }
    }

    public ResponseFaceVerification verificarRostro(String rutaImagen, String idUser,
            String bestPhotoFileName) {
        try {
            String directorioUsuario = "storage/Photo/" + idUser;
            String mejorFotoNombre;

            if (bestPhotoFileName != null && !bestPhotoFileName.isEmpty()) {
                mejorFotoNombre = bestPhotoFileName;
            } else {
                ResponsePhotoFilter filtro = seleccionarMejorFoto(directorioUsuario);
                if (!filtro.isSuccess()) {
                    ResponseFaceVerification error = new ResponseFaceVerification();
                    error.setVerified(false);
                    error.setError(filtro.getError());
                    return error;
                }
                mejorFotoNombre = filtro.getBestImage();
            }

            File mejorFoto = new File(directorioUsuario + "/" + mejorFotoNombre);
            String rutaAbsolutaMejorFoto = mejorFoto.getAbsolutePath();

            File capturaFile = new File(rutaImagen);
            byte[] imageBytes = java.nio.file.Files.readAllBytes(capturaFile.toPath());
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            Map<String, Object> body = new HashMap<>();
            body.put("rutaReferencia", rutaAbsolutaMejorFoto);
            body.put("imagenBase64", base64Image);
            body.put("idUser", idUser);

            String json = postJson("/verificar", body);
            return mapper.readValue(json, ResponseFaceVerification.class);

        } catch (Exception e) {
            ResponseFaceVerification error = new ResponseFaceVerification();
            error.setVerified(false);
            error.setError(e.getMessage());
            return error;
        }
    }

    public ResponseFaceVerification verificarRostroBase64(String base64Image, String idUser,
            String bestPhotoFileName) {
        File tempFile = null;
        try {
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
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
}