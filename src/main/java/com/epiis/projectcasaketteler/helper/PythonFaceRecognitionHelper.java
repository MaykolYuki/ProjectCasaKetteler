package com.epiis.projectcasaketteler.helper;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.springframework.stereotype.Component;

import com.epiis.projectcasaketteler.dto.response.ResponseFaceVerification;

import tools.jackson.databind.ObjectMapper;

@Component
public class PythonFaceRecognitionHelper {
	private static final String PYTHON_PATH = "D:/Proyectos Java/JavaCasaKetteler/projectcasaketteler/python_scripts/venv/Scripts/python.exe";

    private static final String SCRIPT_PATH = "python_scripts/ReconocimientoFacial.py";

    public ResponseFaceVerification verificarRostro(String rutaImagen, String idUser) {
        try {
        	
            String directorioUsuario = "storage/Photo/" + idUser;

            ProcessBuilder processBuilder = new ProcessBuilder(PYTHON_PATH, SCRIPT_PATH, rutaImagen, directorioUsuario);

            processBuilder.redirectErrorStream(true);

            Process process =processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            StringBuilder output = new StringBuilder();

            String line;

            while ((line = reader.readLine()) != null) {
                output.append(line);
            }

            process.waitFor();

            ObjectMapper mapper = new ObjectMapper();

            return mapper.readValue(output.toString(), ResponseFaceVerification.class);

        } catch (Exception e) {

        	ResponseFaceVerification error = new ResponseFaceVerification();

            error.setVerified(false);
            error.setError(e.getMessage());

            return error;
        }
    }
}
