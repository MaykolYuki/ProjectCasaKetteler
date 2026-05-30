package com.epiis.projectcasaketteler.business;

import java.io.File;
import java.net.InetAddress;
import java.util.Date;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.epiis.projectcasaketteler.dto.request.RequestAttendanceInsert;
import com.epiis.projectcasaketteler.dto.response.ResponseFaceVerification;
import com.epiis.projectcasaketteler.entity.EntityAttendance;
import com.epiis.projectcasaketteler.entity.EntityUser;
import com.epiis.projectcasaketteler.helper.ObtainIpAddressHelper;
import com.epiis.projectcasaketteler.helper.PythonFaceRecognitionHelper;
import com.epiis.projectcasaketteler.repository.RepositoryAttendance;
import com.epiis.projectcasaketteler.repository.RepositoryUser;

@Service
public class BusinessAttendance {

    @Autowired
    RepositoryAttendance repositoryAttendance;

    @Autowired
    RepositoryUser repositoryUser;

    @Autowired
    PythonFaceRecognitionHelper pythonFaceRecognitionHelper;

    @Autowired
    ObtainIpAddressHelper obtainIpAddressHelper;

    public ResponseFaceVerification insert(RequestAttendanceInsert request) {
        File tempFile = null;
        ResponseFaceVerification response = new ResponseFaceVerification();

        try {
            // 1. Usamos una ruta bien definida. "temp" es genial, pero asegurémonos de que
            // sea absoluta.
            String tempDir = "temp/";
            File directory = new File(tempDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            String fileName = "captura_" + request.getIdUser() + ".jpg";
            String rutaImagen = tempDir + fileName;
            tempFile = new File(rutaImagen);

            // 2. ¡LA SOLUCIÓN AQUÍ! En lugar de transferTo pasándole el File relativo
            // directo a Tomcat,
            // le pasamos la ruta absoluta real que Windows sí entiende al 100%.
            request.getFile().transferTo(tempFile.getAbsoluteFile()); // <-- AGREGA .getAbsoluteFile()

            ResponseFaceVerification responsetoPython = pythonFaceRecognitionHelper.verificarRostro(rutaImagen,
                    request.getIdUser());
            if (!responsetoPython.isVerified()) {
                responsetoPython.error();
                responsetoPython.getListMessage()
                        .add("Error: Algo salió mal con el reconocimiento facial. Vuelve a intentarlo.");
                return responsetoPython;
            }

            Optional<EntityUser> optionalUser = repositoryUser.findById(request.getIdUser());
            if (!optionalUser.isPresent()) {
                response.error();
                response.getListMessage().add("Error: Usuario no encontrado.");
                return response;
            }
            EntityUser entityUser = optionalUser.get();

            String localIpHost = InetAddress.getLocalHost().getHostAddress();
            String parentResidenceIp = entityUser.getParentResidence().getIpAddress();
            String requestIp = obtainIpAddressHelper.getIp();
            String userLocalAddress = entityUser.getIpAddressLocal();

            System.out.println("=== DEPURACIÓN DE RED ===");
            System.out.println("localIpHost: " + localIpHost);
            System.out.println("parentResidenceIp: " + parentResidenceIp);
            System.out.println("requestIp: " + requestIp);
            System.out.println("userLocalAddress: " + userLocalAddress);
            System.out.println("isSameNetwork (residencia): " + isSameNetwork(localIpHost, parentResidenceIp));
            System.out.println("isSameNetwork (usuario): " + isSameNetwork(requestIp, userLocalAddress));
            // -- FIN DEPURACIÓN --//

            if (!isSameNetwork(localIpHost, parentResidenceIp)) {
                response.error();
                response.getListMessage().add("Error: La Dirección WIFI de la residencia es incorrecta.");
                return response;
            }

            if (!isSameNetwork(requestIp, userLocalAddress)) {
                response.error();
                response.getListMessage().add("Error: Este celular no le pertenece o no está en la red correcta.");
                return response;
            }

            Optional<EntityAttendance> optionalAttendance = repositoryAttendance
                    .findTopByParentUserOrderByCreated_atDesc(entityUser);

            if (optionalAttendance.isPresent()) {
                EntityAttendance lastAttendance = optionalAttendance.get();

                if (lastAttendance.getStatus()) {
                    lastAttendance.setDepartureDate(new java.sql.Date(new Date().getTime()));
                    lastAttendance.setStatus(false);
                    lastAttendance.setUpdated_at(new java.sql.Date(new Date().getTime()));

                    repositoryAttendance.save(lastAttendance);

                    responsetoPython.success();
                    responsetoPython.getListMessage().add("Salida registrada correctamente (Línea completada).");
                    return responsetoPython;
                }
            }

            EntityAttendance newAttendance = new EntityAttendance();
            newAttendance.setIdAtendance(UUID.randomUUID().toString());
            newAttendance.setParentUser(entityUser);
            newAttendance.setEntryDate(new java.sql.Date(new Date().getTime()));
            newAttendance.setStatus(true);
            newAttendance.setCreated_at(new java.sql.Date(new Date().getTime()));

            repositoryAttendance.save(newAttendance);

            responsetoPython.success();
            responsetoPython.getListMessage().add("Entrada registrada correctamente (Nueva línea).");
            return responsetoPython;

        } catch (Exception e) {
            e.printStackTrace();
            response.error();
            response.getListMessage().add("Error: El servicio no funciona por el momento.");
            return response;
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    private String normalizeIpAddress(String ip) {
        if (ip == null)
            return null;

        // Normalizar IPv6 loopback
        if (ip.equals("0:0:0:0:0:0:0:1") || ip.equals("::1")) {
            return "127.0.0.1";
        }

        // Normalizar localhost
        if (ip.equals("localhost")) {
            return "127.0.0.1";
        }

        return ip;
    }

    private boolean isSameNetwork(String ip1, String ip2) {
        String normalizedIp1 = normalizeIpAddress(ip1);
        String normalizedIp2 = normalizeIpAddress(ip2);

        if (normalizedIp1 == null || normalizedIp2 == null)
            return false;

        // Comparación exacta después de normalizar
        return normalizedIp1.equals(normalizedIp2);
    }

    public Map<String, Object> getByUser(String userId) {
        Map<String, Object> res = new HashMap<>();

        Optional<EntityUser> optionalUser = repositoryUser.findById(userId);

        if (!optionalUser.isPresent()) {
            res.put("type", "error");
            res.put("message", "Usuario no encontrado");
            res.put("data", null);
            return res;
        }

        EntityUser entityUser = optionalUser.get();

        List<EntityAttendance> attendances = repositoryAttendance
                .findByParentUserOrderByCreated_atDesc(entityUser);

        res.put("type", "success");
        res.put("message", "Asistencias obtenidas correctamente");
        res.put("data", attendances);

        return res;
    }
}
