package com.epiis.projectcasaketteler.business;

import java.io.File;
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
            request.getFile().transferTo(tempFile.getAbsoluteFile());

            // Obtener usuario PRIMERO
            Optional<EntityUser> optionalUser = repositoryUser.findById(request.getIdUser());
            if (!optionalUser.isPresent()) {
                response.error();
                response.getListMessage().add("Error: Usuario no encontrado.");
                return response;
            }
            EntityUser entityUser = optionalUser.get();

            // Ahora sí, llamar a Python con la referencia cacheada
            ResponseFaceVerification responsetoPython = pythonFaceRecognitionHelper.verificarRostro(
                    rutaImagen, request.getIdUser(), entityUser.getBestPhotoReference());

            // RF-12: umbral de similitud ≥85% — única fuente de verdad
            if (responsetoPython.getSimilarity() < 85.0) {
                responsetoPython.setVerified(false);
                responsetoPython.error();
                responsetoPython.getListMessage().add(
                        "Error: Rostro no reconocido. Similitud: " +
                                String.format("%.1f", responsetoPython.getSimilarity()) + "% (mínimo 85%).");
                return responsetoPython;
            }
            responsetoPython.setVerified(true);

            // RF-13/14: Validación de red por BSSID (preferido) o SSID (fallback)
            String expectedBSSID = entityUser.getParentResidence().getWifiBssid();
            String expectedSSID = entityUser.getParentResidence().getWifiSsid();
            String providedBSSID = request.getBssid();
            String providedSSID = request.getSsid();

            boolean redValida = false;

            if (expectedBSSID != null && !expectedBSSID.isEmpty()) {
                // Si la residencia tiene BSSID configurado, es obligatorio que coincida
                redValida = providedBSSID != null && expectedBSSID.equalsIgnoreCase(providedBSSID);
            } else if (expectedSSID != null && !expectedSSID.isEmpty()) {
                // Fallback a SSID si no hay BSSID configurado
                redValida = providedSSID != null && expectedSSID.equals(providedSSID);
            } else {
                response.error();
                response.getListMessage().add("Error: La residencia no tiene una red WiFi configurada.");
                return response;
            }

            if (!redValida) {
                response.error();
                response.getListMessage().add("Error: Conéctese a la red oficial de la residencia.");
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

                    responsetoPython.setEntrada(false);
                    responsetoPython.success();
                    responsetoPython.getListMessage().add("Salida registrada correctamente.");
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

            responsetoPython.setEntrada(true);
            responsetoPython.success();
            responsetoPython.getListMessage().add("Entrada registrada correctamente.");
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
                // 1. Buscar usuario PRIMERO
                Optional<EntityUser> optionalUser = repositoryUser.findById(record.getIdUser());
                if (!optionalUser.isPresent()) {
                    rechazados++;
                    continue;
                }
                EntityUser entityUser = optionalUser.get();

                // RF-13/14: Validación de red por BSSID y SSID (opcional mientras offline esté
                // en
                // pausa)
                if (record.getBssid() != null && !record.getBssid().isEmpty()) {
                    String expectedBSSID = entityUser.getParentResidence().getWifiBssid();
                    if (expectedBSSID == null || !expectedBSSID.equalsIgnoreCase(record.getBssid())) {
                        rechazados++;
                        continue;
                    }
                } else if (record.getSsid() != null && !record.getSsid().isEmpty()) {
                    String expectedSSID = entityUser.getParentResidence().getWifiSsid();
                    if (expectedSSID == null || !expectedSSID.equals(record.getSsid())) {
                        rechazados++;
                        continue;
                    }
                }

                // 2. Ahora sí, re-verificar con Python usando la referencia cacheada
                ResponseFaceVerification serverResult = pythonFaceRecognitionHelper
                        .verificarRostroBase64(
                                record.getBase64Image(),
                                record.getIdUser(),
                                entityUser.getBestPhotoReference());

                // 3. Aplicar umbral
                if (serverResult.getSimilarity() < 85.0) {
                    rechazados++;
                    continue;
                }

                // 4. Verificar duplicado
                if (isDuplicateSync(entityUser, record.getRecordedAt())) {
                    rechazados++;
                    continue;
                }

                // 5. Guardar con metadatos de auditoría
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
