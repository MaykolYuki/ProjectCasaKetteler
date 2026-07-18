package com.epiis.projectcasaketteler.business;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.epiis.projectcasaketteler.dto.request.RequestAttendanceInsert;
import com.epiis.projectcasaketteler.dto.request.RequestAttendanceSync;
import com.epiis.projectcasaketteler.dto.response.ResponseAttendancePage;
import com.epiis.projectcasaketteler.dto.response.ResponseFaceVerification;
import com.epiis.projectcasaketteler.dto.response.ResponseSyncResult;
import com.epiis.projectcasaketteler.entity.EntityAdmin;
import com.epiis.projectcasaketteler.entity.EntityAttendance;
import com.epiis.projectcasaketteler.entity.EntityUser;
import com.epiis.projectcasaketteler.helper.PythonFaceRecognitionHelper;
import com.epiis.projectcasaketteler.repository.RepositoryAdmin;
import com.epiis.projectcasaketteler.repository.RepositoryAttendance;
import com.epiis.projectcasaketteler.repository.RepositoryUser;

@Service
public class BusinessAttendance {

    @Autowired
    private RepositoryAttendance repositoryAttendance;

    @Autowired
    private RepositoryUser repositoryUser;

    @Autowired
    private PythonFaceRecognitionHelper pythonFaceRecognitionHelper;

    @Autowired
    private RepositoryAdmin repositoryAdmin;

    @Value("${app.temp.path}")
    private String tempPath;

    public ResponseFaceVerification insert(RequestAttendanceInsert request) {
        File tempFile = null;
        ResponseFaceVerification response = new ResponseFaceVerification();

        // Verificar que el servidor de reconocimiento facial esté activo
        if (!pythonFaceRecognitionHelper.isServerRunning()) {
            response.error();
            response.getListMessage()
                    .add("Error: El servicio de reconocimiento facial no está disponible. Contacte al administrador.");
            return response;
        }

        try {
            // 1. Usamos una ruta bien definida. "temp" es genial, pero asegurémonos de que
            // sea absoluta.
            String tempDir = tempPath + "/";
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

            // RF-12: se usa el 'verified' calibrado por el propio modelo (ArcFace),
            // no un porcentaje fijo arbitrario. Python ya trae este valor calculado
            // con su umbral interno de distancia; aquí solo lo respetamos.
            if (!responsetoPython.isVerified()) {
                responsetoPython.error();
                responsetoPython.getListMessage().add(
                        "Error: Rostro no reconocido. Similitud: " +
                                String.format("%.1f", responsetoPython.getSimilarity()) + "%.");
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
            newAttendance.setDescription(request.getDescription());
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

    /**
     * NO USADO — ver nota en AttendanceController.sync(). Conservado por si
     * se retoma el alcance offline en el futuro.
     */
    @Deprecated
    public ResponseSyncResult syncOfflineRecords(String userId, List<RequestAttendanceSync> records) {
        ResponseSyncResult result = new ResponseSyncResult();
        result.setType("success");

        for (RequestAttendanceSync record : records) {
            String idUser = record.getIdUser();
            String recordedAtStr = record.getRecordedAt() != null ? record.getRecordedAt().toString() : "desconocido";

            try {
                // 1. Buscar usuario
                Optional<EntityUser> optionalUser = repositoryUser.findById(idUser);
                if (!optionalUser.isPresent()) {
                    result.agregarRechazado(idUser, recordedAtStr, "Usuario no encontrado");
                    continue;
                }
                EntityUser entityUser = optionalUser.get();

                // 2. Validar red por SSID/BSSID (opcional para offline)
                if (record.getBssid() != null && !record.getBssid().isEmpty()) {
                    String expectedBSSID = entityUser.getParentResidence().getWifiBssid();
                    if (expectedBSSID == null || !expectedBSSID.equalsIgnoreCase(record.getBssid())) {
                        result.agregarRechazado(idUser, recordedAtStr, "BSSID no corresponde a la red oficial");
                        continue;
                    }
                } else if (record.getSsid() != null && !record.getSsid().isEmpty()) {
                    String expectedSSID = entityUser.getParentResidence().getWifiSsid();
                    if (expectedSSID == null || !expectedSSID.equals(record.getSsid())) {
                        result.agregarRechazado(idUser, recordedAtStr, "SSID no corresponde a la red oficial");
                        continue;
                    }
                }

                // 3. Re-verificar con Python
                ResponseFaceVerification serverResult = pythonFaceRecognitionHelper
                        .verificarRostroBase64(
                                record.getBase64Image(),
                                idUser,
                                entityUser.getBestPhotoReference());

                if (!serverResult.isVerified()) {
                    result.agregarRechazado(idUser, recordedAtStr,
                            "Rostro no reconocido. Similitud: " +
                                    String.format("%.1f", serverResult.getSimilarity()) + "%");
                    continue;
                }

                // 4. Verificar duplicado
                if (isDuplicateSync(entityUser, record.getRecordedAt())) {
                    result.agregarRechazado(idUser, recordedAtStr,
                            "Registro duplicado — ya existe uno en ±1 minuto");
                    continue;
                }

                // 5. Guardar
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
                result.agregarProcesado(idUser, recordedAtStr);

            } catch (Exception e) {
                result.agregarRechazado(idUser, recordedAtStr,
                        "Error inesperado: " + e.getMessage());
            }
        }

        return result;
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

    // RF-30/31: Residente consulta su propia asistencia con filtros y paginación
    public Map<String, Object> getByFilters(String userId, String fechaInicio, String fechaFin,
            Boolean estado, int page, int size) {
        Map<String, Object> res = new HashMap<>();

        Optional<EntityUser> optionalUser = repositoryUser.findById(userId);
        if (!optionalUser.isPresent()) {
            res.put("type", "error");
            res.put("message", "Usuario no encontrado");
            return res;
        }

        EntityUser user = optionalUser.get();
        Date inicio = parseFecha(fechaInicio, false);
        Date fin = parseFecha(fechaFin, true);

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);

        org.springframework.data.domain.Page<EntityAttendance> resultado = repositoryAttendance.findByFilters(user,
                inicio, fin, estado, pageable);

        res.put("type", "success");
        res.put("message", "Asistencias obtenidas correctamente");
        res.put("data", new ResponseAttendancePage(resultado));
        return res;
    }

    /**
     * SUPER_ADMIN puede ver cualquier residencia (o todas, si no especifica).
     * ADMIN normal queda forzado a su propia residencia sin importar qué pida.
     */
    private String resolveResidenceScope(String adminId, String requestedIdResidence) {
        Optional<EntityAdmin> adminOpt = repositoryAdmin.findById(adminId);
        if (!adminOpt.isPresent()) {
            return requestedIdResidence;
        }
        EntityAdmin admin = adminOpt.get();
        if (admin.getRole() == EntityAdmin.AdminRole.SUPER_ADMIN) {
            return requestedIdResidence;
        }
        return admin.getParentResidence().getIdResidence();
    }

    // RF-30/31: Admin consulta asistencia de cualquier residente con filtros
    public Map<String, Object> getByFiltersAdmin(String adminId, String idUser, String fechaInicio,
            String fechaFin, Boolean estado, int page, int size) {
        Map<String, Object> res = new HashMap<>();

        Date inicio = parseFecha(fechaInicio, false);
        Date fin = parseFecha(fechaFin, true);
        String idResidence = resolveResidenceScope(adminId, null);

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);

        org.springframework.data.domain.Page<EntityAttendance> resultado = repositoryAttendance
                .findByFiltersAdmin(inicio, fin, estado, idUser, idResidence, pageable);

        res.put("type", "success");
        res.put("message", "Asistencias obtenidas correctamente");
        res.put("data", new ResponseAttendancePage(resultado));
        return res;
    }

    public Map<String, Object> getResumenKPI(String adminId, String idResidenceParam) {
        Map<String, Object> res = new HashMap<>();
        String idResidence = resolveResidenceScope(adminId, idResidenceParam);

        long totalResidentes;
        if (idResidence == null || idResidence.isEmpty()) {
            totalResidentes = repositoryUser.findAll().stream()
                    .filter(u -> Boolean.TRUE.equals(u.getActive()))
                    .count();
        } else {
            totalResidentes = repositoryUser.countActivosByResidencia(idResidence);
        }

        java.time.LocalDate hoy = java.time.LocalDate.now();
        Date inicioDia = java.sql.Date.valueOf(hoy);
        Date finDia = java.sql.Date.valueOf(hoy.plusDays(1));

        long presentes = repositoryAttendance.countPresentesHoy(
                inicioDia, finDia,
                (idResidence == null || idResidence.isEmpty()) ? null : idResidence);
        long ausentes = totalResidentes - presentes;

        res.put("type", "success");
        res.put("totalResidentes", totalResidentes);
        res.put("presentes", presentes);
        res.put("ausentes", ausentes < 0 ? 0 : ausentes);
        res.put("fecha", hoy.toString());

        return res;
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

    public PythonFaceRecognitionHelper getPythonFaceRecognitionHelper() {
        return pythonFaceRecognitionHelper;
    }
}
