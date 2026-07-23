package com.epiis.projectcasaketteler.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.epiis.projectcasaketteler.dto.request.RequestLogin;
import com.epiis.projectcasaketteler.dto.response.ResponseLogin;
import com.epiis.projectcasaketteler.entity.EntityUser;
import com.epiis.projectcasaketteler.helper.JwtHelper;
import com.epiis.projectcasaketteler.helper.ObtainIpAddressHelper;
import com.epiis.projectcasaketteler.helper.PasswordEncoderHelper;
import com.epiis.projectcasaketteler.repository.RepositoryAdmin;
import com.epiis.projectcasaketteler.repository.RepositoryUser;

/**
 * Reglas de seguridad del inicio de sesión: bloqueo por intentos fallidos,
 * cuentas desactivadas y sesión única por usuario.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Inicio de sesión")
class BusinessUserLoginTest {

	private static final String CORREO = "residente@casaketteler.pe";
	private static final String CLAVE_CORRECTA = "clave-correcta";

	@Mock
	private RepositoryUser repositoryUser;

	@Mock
	private RepositoryAdmin repositoryAdmin;

	@Mock
	private PasswordEncoderHelper passwordEncoderHelper;

	@Mock
	private ObtainIpAddressHelper obtainIpAddressHelper;

	@Mock
	private JwtHelper jwtHelper;

	@InjectMocks
	private BusinessUser businessUser;

	private EntityUser residente;

	@BeforeEach
	void setUp() {
		BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

		residente = new EntityUser();
		residente.setIdUser("residente-1");
		residente.setEmail(CORREO);
		residente.setPassword(encoder.encode(CLAVE_CORRECTA));
		residente.setActive(true);
		residente.setLoginAttempts(0);
		residente.setRole(EntityUser.UserRole.RESIDENTE);

		when(passwordEncoderHelper.passwordEncoder()).thenReturn(encoder);
		when(repositoryAdmin.findByEmail(anyString())).thenReturn(Optional.empty());
		when(repositoryUser.findByEmail(CORREO)).thenReturn(Optional.of(residente));
		when(obtainIpAddressHelper.getIp()).thenReturn("192.168.1.50");
		when(jwtHelper.generateToken(anyString(), anyString(), anyString())).thenReturn("token-de-prueba");
	}

	private RequestLogin intentoCon(String clave) {
		RequestLogin solicitud = new RequestLogin();
		solicitud.setEmail(CORREO);
		solicitud.setPassword(clave);
		return solicitud;
	}

	@Test
	@DisplayName("Con la contraseña correcta entrega un token y limpia los intentos fallidos")
	void iniciaSesionConCredencialesValidas() {
		residente.setLoginAttempts(3);

		ResponseLogin respuesta = businessUser.login(intentoCon(CLAVE_CORRECTA));

		assertThat(respuesta.getType()).isEqualTo("success");
		assertThat(respuesta.getToken()).isEqualTo("token-de-prueba");
		assertThat(residente.getLoginAttempts()).isZero();
	}

	@Test
	@DisplayName("Sesión única: al entrar se invalidan las sesiones abiertas en otros dispositivos")
	void invalidaSesionesAnterioresAlIniciarSesion() {
		Date antesDelLogin = new Date();

		businessUser.login(intentoCon(CLAVE_CORRECTA));

		// tokenValidAfter se mueve al momento del login: los tokens emitidos antes
		// dejan de ser válidos (el filtro JWT los rechaza).
		assertThat(residente.getTokenValidAfter()).isNotNull();
		assertThat(residente.getTokenValidAfter())
				.isCloseTo(antesDelLogin, 60_000L); // margen amplio para el CI
	}

	@Test
	@DisplayName("Con la contraseña incorrecta no entrega token y cuenta el intento")
	void rechazaCredencialesIncorrectas() {
		ResponseLogin respuesta = businessUser.login(intentoCon("clave-equivocada"));

		assertThat(respuesta.getType()).isEqualTo("error");
		assertThat(respuesta.getToken()).isNull();
		assertThat(residente.getLoginAttempts()).isEqualTo(1);
	}

	@Test
	@DisplayName("Al quinto intento fallido bloquea la cuenta temporalmente")
	void bloqueaLaCuentaTrasCincoIntentos() {
		residente.setLoginAttempts(4); // este será el quinto

		ResponseLogin respuesta = businessUser.login(intentoCon("clave-equivocada"));

		assertThat(respuesta.getListMessage()).anyMatch(m -> m.contains("bloqueada"));
		assertThat(residente.getLockedUntil()).isAfter(new Date());
	}

	@Test
	@DisplayName("Una cuenta bloqueada no puede entrar aunque la contraseña sea correcta")
	void noPermiteEntrarConCuentaBloqueada() {
		residente.setLockedUntil(new Date(System.currentTimeMillis() + 10 * 60_000L));

		ResponseLogin respuesta = businessUser.login(intentoCon(CLAVE_CORRECTA));

		assertThat(respuesta.getType()).isEqualTo("error");
		assertThat(respuesta.getListMessage()).anyMatch(m -> m.contains("bloqueada"));
		verify(jwtHelper, never()).generateToken(any(), any(), any());
	}

	@Test
	@DisplayName("Una cuenta desactivada no puede iniciar sesión")
	void noPermiteEntrarConCuentaDesactivada() {
		residente.setActive(false);

		ResponseLogin respuesta = businessUser.login(intentoCon(CLAVE_CORRECTA));

		assertThat(respuesta.getType()).isEqualTo("error");
		verify(jwtHelper, never()).generateToken(any(), any(), any());
	}
}
