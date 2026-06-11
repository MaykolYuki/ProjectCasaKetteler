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
import com.epiis.projectcasaketteler.dto.request.RequestAttendanceSync;
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

            // RF-12: umbral de similitud ≥85%
            if (!responsetoPython.isVerified() || responsetoPython.getSimilarity() < 85.0) {
                responsetoPython.error();
                responsetoPython.getListMessage().add(
                        "Error: Rostro no reconocido. Similitud: " +
                                String.format("%.1f", responsetoPython.getSimilarity()) + "% (mínimo 85%).");
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

            // RF-10: cooldown de 5 minutos entre registros
            if (optionalAttendance.isPresent()) {
                EntityAttendance lastAttendance = optionalAttendance.get();

                Date lastTime = lastAttendance.getCreated_at();
                long diffMinutes = (new Date().getTime() - lastTime.getTime()) / (1000 * 60);

                if (diffMinutes < 5) {
                    long restante = 5 - diffMinutes;
                    responsetoPython.error();
                    responsetoPython.getListMessage().add(
                            "Error: Debes esperar " + restante + " minuto(s) para registrar nuevamente.");
                    return responsetoPython;
                }
            }

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

    public Map<String, Object> syncOfflineRecords(String userId, List<RequestAttendanceSync> records) {
        Map<String, Object> res = new HashMap<>();
        int procesados = 0;
        int rechazados = 0;

        for (RequestAttendanceSync record : records) {
            try {
                // 1. Re-verificar con Python
                ResponseFaceVerification serverResult = pythonFaceRecognitionHelper
                        .verificarRostroBase64(record.getBase64Image(), record.getIdUser());

                // 2. Aplicar umbral del servidor (RF-12)
                if (!serverResult.isVerified() || serverResult.getSimilarity() < 85.0) {
                    rechazados++;
                    continue;
                }

                // 3. Verificar duplicado (mismo usuario, misma hora ±1 min)
                Optional<EntityUser> optionalUser = repositoryUser.findById(record.getIdUser());
                if (!optionalUser.isPresent()) {
                    rechazados++;
                    continue;
                }
                EntityUser entityUser = optionalUser.get();

                if (isDuplicateSync(entityUser, record.getRecordedAt())) {
                    rechazados++;
                    continue;
                }

                // 4. Guardar con metadatos de auditoría
                EntityAttendance attendance = new EntityAttendance();
                attendance.setIdAtendance(UUID.randomUUID().toString());
                attendance.setParentUser(entityUser);
                attendance.setRecordedAt(record.getRecordedAt());
                attendance.setSyncedAt(new Date());
                attendance.setClientSimilarity(record.getClientSimilarity());
                attendance.setServerSimilarity(serverResult.getSimilarity());
                attendance.setVerifiedByServer(true);
                attendance.setStatus(record.getIsEntry());
                attendance.setCreated_at(new Date());

                repositoryAttendance.save(attendance);
                procesados++;

            } catch (Exception e) {
                rechazados++;
            }
        }

        res.put("type", "success");
        res.put("procesados", procesados);
        res.put("rechazados", rechazados);
        return res;
    }

    private boolean isDuplicateSync(EntityUser user, Date recordedAt) {
        List<EntityAttendance> recientes = repositoryAttendance
                .findByParentUserOrderByCreated_atDesc(user);

        for (EntityAttendance a : recientes) {
            if (a.getRecordedAt() == null)
                continue;
            long diffMs = Math.abs(a.getRecordedAt().getTime() - recordedAt.getTime());
            long diffMin = diffMs / (1000 * 60);
            if (diffMin <= 1)
                return true;
        }
        return false;
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
