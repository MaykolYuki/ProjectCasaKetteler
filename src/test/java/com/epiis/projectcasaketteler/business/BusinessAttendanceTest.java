package com.epiis.projectcasaketteler.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import com.epiis.projectcasaketteler.dto.request.RequestAttendanceInsert;
import com.epiis.projectcasaketteler.dto.response.ResponseFaceVerification;
import com.epiis.projectcasaketteler.entity.EntityAttendance;
import com.epiis.projectcasaketteler.entity.EntityAttendance.AttendanceEventType;
import com.epiis.projectcasaketteler.entity.EntityResidence;
import com.epiis.projectcasaketteler.entity.EntityUser;
import com.epiis.projectcasaketteler.helper.PythonFaceRecognitionHelper;
import com.epiis.projectcasaketteler.repository.RepositoryAdmin;
import com.epiis.projectcasaketteler.repository.RepositoryAttendance;
import com.epiis.projectcasaketteler.repository.RepositoryUser;

/**
 * Reglas de negocio del registro de asistencia: alternancia entrada/salida,
 * el tiempo de espera entre marcas, la detección de anomalías y el intento fallido.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Registro de asistencia")
class BusinessAttendanceTest {

	private static final String BSSID_RESIDENCIA = "AA:BB:CC:DD:EE:FF";

	@Mock
	private RepositoryAttendance repositoryAttendance;

	@Mock
	private RepositoryUser repositoryUser;

	@Mock
	private PythonFaceRecognitionHelper pythonFaceRecognitionHelper;

	@Mock
	private RepositoryAdmin repositoryAdmin;

	@InjectMocks
	private BusinessAttendance businessAttendance;

	private EntityUser residente;

	@BeforeEach
	void setUp(@TempDir Path carpetaTemporal) {
		ReflectionTestUtils.setField(businessAttendance, "tempPath", carpetaTemporal.toString());

		EntityResidence residencia = new EntityResidence();
		residencia.setWifiBssid(BSSID_RESIDENCIA);

		residente = new EntityUser();
		residente.setIdUser("residente-1");
		residente.setParentResidence(residencia);

		when(repositoryUser.findById("residente-1")).thenReturn(Optional.of(residente));
		when(pythonFaceRecognitionHelper.isServerRunning()).thenReturn(true);
		when(repositoryAttendance.findLastFailedAttempt(any())).thenReturn(Optional.empty());
	}

	// --- Utilidades ---

	private void rostroReconocido() {
		ResponseFaceVerification verificacion = new ResponseFaceVerification();
		verificacion.setVerified(true);
		verificacion.setSimilarity(78.0);
		when(pythonFaceRecognitionHelper.verificarRostro(any(), anyString(), any()))
				.thenReturn(verificacion);
	}

	private void rostroNoReconocido() {
		ResponseFaceVerification verificacion = new ResponseFaceVerification();
		verificacion.setVerified(false);
		verificacion.setSimilarity(31.5);
		when(pythonFaceRecognitionHelper.verificarRostro(any(), anyString(), any()))
				.thenReturn(verificacion);
	}

	private RequestAttendanceInsert solicitudDesdeLaResidencia() {
		RequestAttendanceInsert solicitud = new RequestAttendanceInsert();
		solicitud.setIdUser("residente-1");
		solicitud.setFiles(new MultipartFile[] { mock(MultipartFile.class) });
		solicitud.setBssid(BSSID_RESIDENCIA);
		solicitud.setDescription("Clases");
		return solicitud;
	}

	private void ultimaMarcaFue(AttendanceEventType tipo, int haceMinutos) {
		EntityAttendance ultima = new EntityAttendance();
		ultima.setEventType(tipo);
		ultima.setEventTimestamp(new Date(System.currentTimeMillis() - haceMinutos * 60_000L));
		when(repositoryAttendance.findTopByParentUserOrderByEventTimestampDesc(residente))
				.thenReturn(Optional.of(ultima));
	}

	private EntityAttendance eventoGuardado() {
		ArgumentCaptor<EntityAttendance> captor = ArgumentCaptor.forClass(EntityAttendance.class);
		verify(repositoryAttendance).save(captor.capture());
		return captor.getValue();
	}

	// --- Pruebas ---

	@Test
	@DisplayName("Si el residente estaba fuera, su marca registra una ENTRADA y queda presente")
	void registraEntradaCuandoEstabaFuera() {
		residente.setPresente(false);
		when(repositoryAttendance.findTopByParentUserOrderByEventTimestampDesc(residente))
				.thenReturn(Optional.empty());
		rostroReconocido();

		businessAttendance.insert(solicitudDesdeLaResidencia());

		assertThat(eventoGuardado().getEventType()).isEqualTo(AttendanceEventType.ENTRADA);
		assertThat(residente.getPresente()).isTrue();
	}

	@Test
	@DisplayName("Si el residente estaba dentro, su marca registra una SALIDA y queda ausente")
	void registraSalidaCuandoEstabaDentro() {
		residente.setPresente(true);
		when(repositoryAttendance.findTopByParentUserOrderByEventTimestampDesc(residente))
				.thenReturn(Optional.empty());
		rostroReconocido();

		businessAttendance.insert(solicitudDesdeLaResidencia());

		assertThat(eventoGuardado().getEventType()).isEqualTo(AttendanceEventType.SALIDA);
		assertThat(residente.getPresente()).isFalse();
	}

	@Test
	@DisplayName("No permite marcar de nuevo antes de 5 minutos (evita marcas duplicadas)")
	void rechazaMarcaAntesDelTiempoDeEspera() {
		residente.setPresente(false);
		ultimaMarcaFue(AttendanceEventType.ENTRADA, 2);
		rostroReconocido();

		ResponseFaceVerification respuesta = businessAttendance.insert(solicitudDesdeLaResidencia());

		assertThat(respuesta.getListMessage()).anyMatch(m -> m.contains("esperar"));
		verify(repositoryAttendance, never()).save(any());
	}

	@Test
	@DisplayName("Marca como anomalía dos eventos seguidos del mismo tipo")
	void marcaAnomaliaSiSeRepiteElTipoDeEvento() {
		// Estaba fuera -> su marca sería ENTRADA, pero la anterior ya fue ENTRADA.
		residente.setPresente(false);
		ultimaMarcaFue(AttendanceEventType.ENTRADA, 30);
		rostroReconocido();

		businessAttendance.insert(solicitudDesdeLaResidencia());

		EntityAttendance evento = eventoGuardado();
		assertThat(evento.getEventType()).isEqualTo(AttendanceEventType.ENTRADA);
		assertThat(evento.getEsAnomalia()).isTrue();
	}

	@Test
	@DisplayName("Una marca normal no se marca como anomalía")
	void noMarcaAnomaliaEnSecuenciaCorrecta() {
		residente.setPresente(true); // -> SALIDA, y la anterior fue ENTRADA
		ultimaMarcaFue(AttendanceEventType.ENTRADA, 30);
		rostroReconocido();

		businessAttendance.insert(solicitudDesdeLaResidencia());

		assertThat(eventoGuardado().getEsAnomalia()).isFalse();
	}

	@Test
	@DisplayName("Si el rostro no coincide, guarda un INTENTO_FALLIDO y no registra asistencia")
	void registraIntentoFallidoSiElRostroNoCoincide() {
		residente.setPresente(false);
		rostroNoReconocido();

		ResponseFaceVerification respuesta = businessAttendance.insert(solicitudDesdeLaResidencia());

		EntityAttendance evento = eventoGuardado();
		assertThat(evento.getEventType()).isEqualTo(AttendanceEventType.INTENTO_FALLIDO);
		assertThat(evento.getMotivoFallo()).contains("Rostro no reconocido");
		assertThat(respuesta.isVerified()).isFalse();
	}

	@Test
	@DisplayName("Rechaza la marca si el celular no está en la red de la residencia")
	void rechazaMarcaDesdeOtraRed() {
		residente.setPresente(false);
		rostroReconocido();

		RequestAttendanceInsert desdeOtraRed = solicitudDesdeLaResidencia();
		desdeOtraRed.setBssid("11:22:33:44:55:66"); // otra red

		ResponseFaceVerification respuesta = businessAttendance.insert(desdeOtraRed);

		assertThat(eventoGuardado().getEventType()).isEqualTo(AttendanceEventType.INTENTO_FALLIDO);
		assertThat(respuesta.getListMessage()).anyMatch(m -> m.contains("red oficial"));
	}
}
