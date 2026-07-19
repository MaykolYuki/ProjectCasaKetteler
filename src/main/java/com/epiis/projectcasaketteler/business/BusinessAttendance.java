package com.epiis.projectcasaketteler.business;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.epiis.projectcasaketteler.dto.request.RequestAttendanceInsert;
import com.epiis.projectcasaketteler.dto.response.ResponseAttendancePage;
import com.epiis.projectcasaketteler.dto.response.ResponseFaceVerification;
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
        List<File> tempFilesRafaga = new ArrayList<>();
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

            // Obtener usuario PRIMERO
            Optional<EntityUser> optionalUser = repositoryUser.findById(request.getIdUser());
            if (!optionalUser.isPresent()) {
                response.error();
                response.getListMessage().add("Error: Usuario no encontrado.");
                return response;
            }
            EntityUser entityUser = optionalUser.get();

            // Guardar las N capturas de la ráfaga en temp. En lugar de transferTo
            // pasándole el File relativo directo a Tomcat, le pasamos la ruta absoluta
            // real que Windows sí entiende al 100%.
            MultipartFile[] files = request.getFiles();
            if (files == null || files.length == 0) {
                response.error();
                response.getListMessage().add("Error: No se recibió ninguna captura.");
                return response;
            }

            List<String> rutasCapturas = new ArrayList<>();
            for (int i = 0; i < files.length; i++) {
                String fileName = "captura_" + request.getIdUser() + "_" + i + ".jpg";
                File f = new File(tempDir + fileName);
                files[i].transferTo(f.getAbsoluteFile());
                rutasCapturas.add(f.getAbsolutePath());
                tempFilesRafaga.add(f);
            }

            // Ahora sí, llamar a Python con la referencia cacheada
            ResponseFaceVerification responsetoPython = pythonFaceRecognitionHelper.verificarRostro(
                    rutasCapturas.toArray(new String[0]), request.getIdUser(), entityUser.getBestPhotoReference());

            // RF-12: verificación calibrada del modelo (verified de ArcFace)
            boolean identidadValida = responsetoPython.isVerified();

            // RF-13/14: Validación de red por BSSID (preferido) o SSID (fallback)
            String expectedBSSID = entityUser.getParentResidence().getWifiBssid();
            String expectedSSID = entityUser.getParentResidence().getWifiSsid();
            String providedBSSID = request.getBssid();
            String providedSSID = request.getSsid();

            boolean redValida = false;
            boolean redConfigurada = true;

            if (expectedBSSID != null && !expectedBSSID.isEmpty()) {
                redValida = providedBSSID != null && expectedBSSID.equalsIgnoreCase(providedBSSID);
            } else if (expectedSSID != null && !expectedSSID.isEmpty()) {
                redValida = providedSSID != null && expectedSSID.equals(providedSSID);
            } else {
                redConfigurada = false;
            }

            if (!redConfigurada) {
                response.error();
                response.getListMessage().add("Error: La residencia no tiene una red WiFi configurada.");
                return response;
            }

            // Si falla identidad o red, registrar INTENTO_FALLIDO (con anti-spam)
            if (!identidadValida || !redValida) {
                String motivo = !identidadValida
                        ? "Rostro no reconocido (similitud: " + String.format("%.1f", responsetoPython.getSimilarity())
                                + "%)"
                        : "Red no autorizada";

                registrarIntentoFallido(entityUser, motivo, providedSSID, providedBSSID,
                        responsetoPython.getSimilarity(), request.getDescription());

                responsetoPython.setVerified(identidadValida);
                responsetoPython.error();
                responsetoPython.getListMessage().add(
                        !identidadValida
                                ? "Error: Rostro no reconocido. Similitud: "
                                        + String.format("%.1f", responsetoPython.getSimilarity()) + "%."
                                : "Error: Conéctese a la red oficial de la residencia.");
                return responsetoPython;
            }

            responsetoPython.setVerified(true);

            // RF-10: cooldown de 5 minutos entre eventos exitosos
            Optional<EntityAttendance> optionalLast = repositoryAttendance
                    .findTopByParentUserOrderByEventTimestampDesc(entityUser);

            if (optionalLast.isPresent()) {
                EntityAttendance last = optionalLast.get();
                if (last.getEventType() != EntityAttendance.AttendanceEventType.INTENTO_FALLIDO) {
                    long diffMinutes = (new Date().getTime() - last.getEventTimestamp().getTime()) / (1000 * 60);
                    if (diffMinutes < 5) {
                        long restante = 5 - diffMinutes;
                        responsetoPython.error();
                        responsetoPython.getListMessage().add(
                                "Error: Debes esperar " + restante + " minuto(s) para registrar nuevamente.");
                        return responsetoPython;
                    }
                }
            }

            // Determinar tipo de evento según el estado de presencia
            boolean estabaPresente = Boolean.TRUE.equals(entityUser.getPresente());
            EntityAttendance.AttendanceEventType tipoEvento = estabaPresente
                    ? EntityAttendance.AttendanceEventType.SALIDA
                    : EntityAttendance.AttendanceEventType.ENTRADA;

            // Detección de anomalía: dos salidas o dos entradas seguidas
            boolean esAnomalia = false;
            if (optionalLast.isPresent()) {
                EntityAttendance last = optionalLast.get();
                if (last.getEventType() == tipoEvento) {
                    esAnomalia = true;
                }
            }

            EntityAttendance evento = new EntityAttendance();
            evento.setIdAtendance(UUID.randomUUID().toString());
            evento.setParentUser(entityUser);
            evento.setEventTimestamp(new Date());
            evento.setEventType(tipoEvento);
            evento.setEsAnomalia(esAnomalia);
            evento.setDescription(request.getDescription());
            evento.setSsid(providedSSID);
            evento.setBssid(providedBSSID);
            evento.setServerSimilarity(responsetoPython.getSimilarity());
            evento.setCreated_at(new Date());

            repositoryAttendance.save(evento);

            // Actualizar el estado de presencia del residente
            entityUser.setPresente(tipoEvento == EntityAttendance.AttendanceEventType.ENTRADA);
            repositoryUser.save(entityUser);

            responsetoPython.setEntrada(tipoEvento == EntityAttendance.AttendanceEventType.ENTRADA);
            responsetoPython.success();

            String mensajeExito = tipoEvento == EntityAttendance.AttendanceEventType.ENTRADA
                    ? "Entrada registrada correctamente."
                    : "Salida registrada correctamente.";
            if (esAnomalia) {
                mensajeExito += " (Registro marcado como anomalía para revisión del administrador.)";
            }
            responsetoPython.getListMessage().add(mensajeExito);
            return responsetoPython;

        } catch (Exception e) {
            e.printStackTrace();
            response.error();
            response.getListMessage().add("Error: El servicio no funciona por el momento.");
            return response;
        } finally {
            for (File f : tempFilesRafaga) {
                if (f.exists()) {
                    f.delete();
                }
            }
        }
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
                .findByParentUserOrderByEventTimestampDesc(entityUser);

        res.put("type", "success");
        res.put("message", "Asistencias obtenidas correctamente");
        res.put("data", attendances);

        return res;
    }

    // RF-30/31: Residente consulta su propia asistencia con filtros y paginación
    public Map<String, Object> getByFilters(String userId, String fechaInicio, String fechaFin,
            String tipo, int page, int size) {
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
        EntityAttendance.AttendanceEventType tipoEvento = parseTipo(tipo);

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);

        org.springframework.data.domain.Page<EntityAttendance> resultado = repositoryAttendance.findByFilters(user,
                inicio, fin, tipoEvento, pageable);

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
            String fechaFin, String tipo, int page, int size) {
        Map<String, Object> res = new HashMap<>();

        Date inicio = parseFecha(fechaInicio, false);
        Date fin = parseFecha(fechaFin, true);
        String idResidence = resolveResidenceScope(adminId, null);
        EntityAttendance.AttendanceEventType tipoEvento = parseTipo(tipo);

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);

        org.springframework.data.domain.Page<EntityAttendance> resultado = repositoryAttendance
                .findByFiltersAdmin(inicio, fin, tipoEvento, idUser, idResidence, pageable);

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

        long presentes;
        if (idResidence == null || idResidence.isEmpty()) {
            presentes = repositoryUser.findAll().stream()
                    .filter(u -> Boolean.TRUE.equals(u.getActive()) && Boolean.TRUE.equals(u.getPresente()))
                    .count();
        } else {
            presentes = repositoryUser.countPresentesByResidencia(idResidence);
        }
        long ausentes = totalResidentes - presentes;

        res.put("type", "success");
        res.put("totalResidentes", totalResidentes);
        res.put("presentes", presentes);
        res.put("ausentes", ausentes < 0 ? 0 : ausentes);
        res.put("fecha", java.time.LocalDate.now().toString());

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

    private void registrarIntentoFallido(EntityUser user, String motivo, String ssid, String bssid,
            double similarity, String description) {
        // Anti-spam: no registrar si ya hay un intento fallido en los últimos 2 minutos
        Optional<EntityAttendance> ultimoFallo = repositoryAttendance.findLastFailedAttempt(user);
        if (ultimoFallo.isPresent()) {
            long diffMinutes = (new Date().getTime() - ultimoFallo.get().getEventTimestamp().getTime()) / (1000 * 60);
            if (diffMinutes < 2) {
                return; // ya hay un fallo reciente, no inundar la tabla
            }
        }

        EntityAttendance fallido = new EntityAttendance();
        fallido.setIdAtendance(UUID.randomUUID().toString());
        fallido.setParentUser(user);
        fallido.setEventTimestamp(new Date());
        fallido.setEventType(EntityAttendance.AttendanceEventType.INTENTO_FALLIDO);
        fallido.setEsAnomalia(true); // todo intento fallido es una anomalía a revisar
        fallido.setMotivoFallo(motivo);
        fallido.setSsid(ssid);
        fallido.setBssid(bssid);
        fallido.setServerSimilarity(similarity);
        fallido.setDescription(description);
        fallido.setCreated_at(new Date());
        repositoryAttendance.save(fallido);
    }

    private EntityAttendance.AttendanceEventType parseTipo(String tipo) {
        if (tipo == null || tipo.isEmpty())
            return null;
        try {
            return EntityAttendance.AttendanceEventType.valueOf(tipo);
        } catch (Exception e) {
            return null;
        }
    }
}
