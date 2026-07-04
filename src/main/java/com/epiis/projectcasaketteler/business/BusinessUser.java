package com.epiis.projectcasaketteler.business;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.epiis.projectcasaketteler.dto.request.RequestChangePassword;
import com.epiis.projectcasaketteler.dto.request.RequestDeactivateUser;
import com.epiis.projectcasaketteler.dto.request.RequestLogin;
import com.epiis.projectcasaketteler.dto.request.RequestUserInsert;
import com.epiis.projectcasaketteler.dto.request.RequestUserUpdate;
import com.epiis.projectcasaketteler.dto.request.RequestUserUpdatePassword;
import com.epiis.projectcasaketteler.dto.response.ResponseLogin;
import com.epiis.projectcasaketteler.dto.response.ResponseUserDeleteById;
import com.epiis.projectcasaketteler.dto.response.ResponseUserGetAll;
import com.epiis.projectcasaketteler.dto.response.ResponseUserGetById;
import com.epiis.projectcasaketteler.dto.response.ResponseUserInsert;
import com.epiis.projectcasaketteler.dto.response.ResponseUserUpdate;
import com.epiis.projectcasaketteler.dto.response.ResponseUserUpdatePassword;
import com.epiis.projectcasaketteler.entity.EntityAdmin;
import com.epiis.projectcasaketteler.entity.EntityResidence;
import com.epiis.projectcasaketteler.entity.EntityUser;
import com.epiis.projectcasaketteler.entity.EntityUser.UserRole;
import com.epiis.projectcasaketteler.helper.EmailHelper;
import com.epiis.projectcasaketteler.helper.JwtHelper;
import com.epiis.projectcasaketteler.helper.ObtainIpAddressHelper;
import com.epiis.projectcasaketteler.helper.PasswordEncoderHelper;
import com.epiis.projectcasaketteler.repository.RepositoryAdmin;
import com.epiis.projectcasaketteler.repository.RepositoryUser;

@Service
public class BusinessUser {
	@Autowired
	RepositoryUser repositoryUser;

	@Autowired
	RepositoryAdmin repositoryAdmin;

	@Autowired
	PasswordEncoderHelper passwordEncoderHelper;

	@Autowired
	ObtainIpAddressHelper obtainIpAddressHelper;

	@Autowired
	private JwtHelper jwtHelper;

	@Autowired
	private EmailHelper emailHelper;

	private BCryptPasswordEncoder passwordEncoder() {
		return passwordEncoderHelper.passwordEncoder();
	}

	// LOGIN CORREGIDO
	public ResponseLogin login(RequestLogin request) {
		ResponseLogin response = new ResponseLogin();

		try {
			// PRIMERO: Buscar en ADMIN
			Optional<EntityAdmin> adminOptional = repositoryAdmin.findByEmail(request.getEmail());

			if (adminOptional.isPresent()) {
				EntityAdmin admin = adminOptional.get();

				// Verificar si está bloqueado
				if (admin.getLockedUntil() != null && admin.getLockedUntil().after(new Date())) {
					long minutosRestantes = (admin.getLockedUntil().getTime() - new Date().getTime()) / (1000 * 60);
					response.setType("error");
					response.getListMessage().add("Cuenta bloqueada. Intente en " + minutosRestantes + " minuto(s).");
					return response;
				}

				if (admin.getActive() == null || !admin.getActive()) {
					response.setType("error");
					response.getListMessage().add("Usuario desactivado");
					return response;
				}

				if (!passwordEncoder().matches(request.getPassword(), admin.getPassword())) {
					int intentos = admin.getLoginAttempts() == null ? 0 : admin.getLoginAttempts();
					intentos++;
					admin.setLoginAttempts(intentos);

					if (intentos >= 5) {
						Date bloqueoHasta = new Date(new Date().getTime() + 15 * 60 * 1000);
						admin.setLockedUntil(bloqueoHasta);
						admin.setLoginAttempts(0);
						repositoryAdmin.save(admin);
						response.setType("error");
						response.getListMessage().add("Cuenta bloqueada por 15 minutos tras 5 intentos fallidos.");
						return response;
					}

					repositoryAdmin.save(admin);
					response.setType("error");
					response.getListMessage().add("Credenciales incorrectas. Intento " + intentos + " de 5.");
					return response;
				}

				// Login exitoso — resetear intentos
				admin.setLoginAttempts(0);
				admin.setLockedUntil(null);
				repositoryAdmin.save(admin);

				String token = jwtHelper.generateToken(admin.getIdAdmin(), admin.getEmail(),
						admin.getRole().toString());

				response.setType("success");
				response.setToken(token);
				response.setUserId(admin.getIdAdmin());
				response.setRole(admin.getRole().toString());
				response.setFirstName(admin.getFirstName());
				response.setSurName(admin.getSurName());
				response.setFirstLogin(false);
				response.getListMessage().add("Login exitoso");
				return response;
			}

			// SEGUNDO: Buscar en USER
			Optional<EntityUser> userOptional = repositoryUser.findByEmail(request.getEmail());

			if (userOptional.isPresent()) {
				EntityUser user = userOptional.get();

				// Verificar si está bloqueado
				if (user.getLockedUntil() != null && user.getLockedUntil().after(new Date())) {
					long minutosRestantes = (user.getLockedUntil().getTime() - new Date().getTime()) / (1000 * 60);
					response.setType("error");
					response.getListMessage().add("Cuenta bloqueada. Intente en " + minutosRestantes + " minuto(s).");
					return response;
				}

				if (user.getActive() == null || !user.getActive()) {
					response.setType("error");
					response.getListMessage().add("Usuario desactivado");
					return response;
				}

				if (!passwordEncoder().matches(request.getPassword(), user.getPassword())) {
					int intentos = user.getLoginAttempts() == null ? 0 : user.getLoginAttempts();
					intentos++;
					user.setLoginAttempts(intentos);

					if (intentos >= 5) {
						Date bloqueoHasta = new Date(new Date().getTime() + 15 * 60 * 1000);
						user.setLockedUntil(bloqueoHasta);
						user.setLoginAttempts(0);
						repositoryUser.save(user);
						response.setType("error");
						response.getListMessage().add("Cuenta bloqueada por 15 minutos tras 5 intentos fallidos.");
						return response;
					}

					repositoryUser.save(user);
					response.setType("error");
					response.getListMessage().add("Credenciales incorrectas. Intento " + intentos + " de 5.");
					return response;
				}

				// Login exitoso — resetear intentos
				user.setLoginAttempts(0);
				user.setLockedUntil(null);
				repositoryUser.save(user);

				String token = jwtHelper.generateToken(user.getIdUser(), user.getEmail(),
						user.getRole().toString());

				response.setType("success");
				response.setToken(token);
				response.setUserId(user.getIdUser());
				response.setRole(user.getRole().toString());
				response.setFirstName(user.getFirstName());
				response.setSurName(user.getSurName());
				response.setFirstLogin(user.getFirstLogin());
				response.getListMessage().add("Login exitoso");
				return response;
			}

			// No encontrado en ninguna tabla
			response.setType("error");
			response.getListMessage().add("Credenciales incorrectas");
			return response;

		} catch (Exception e) {
			response.setType("error");
			response.getListMessage().add("Error en el login: " + e.getMessage());
			return response;
		}
	}

	// REGISTRO CORREGIDO
	public ResponseUserInsert insert(RequestUserInsert request) {
		ResponseUserInsert response = new ResponseUserInsert();

		try {
			if (repositoryUser.findByEmail(request.getEmail()).isPresent()) {
				response.setType("error");
				response.getListMessage().add("El email ya está registrado");
				return response;
			}

			String temporalPassword = generateTemporalPassword();

			EntityUser entityUser = new EntityUser();
			EntityResidence entityResidence = new EntityResidence();
			entityResidence.setIdResidence(request.getIdResidence());

			entityUser.setIdUser(UUID.randomUUID().toString());
			entityUser.setParentResidence(entityResidence);
			entityUser.setFirstName(request.getFirstName());
			entityUser.setSurName(request.getSurName());
			entityUser.setEmail(request.getEmail());
			entityUser.setPassword(passwordEncoder().encode(temporalPassword));

			// Convertir int a String
			entityUser.setCellPhoneNumber(String.valueOf(request.getCellPhoneNumber()));
			entityUser.setCellPhoneEmergency(String.valueOf(request.getCellPhoneEmergency()));

			entityUser.setIpAddressLocal(obtainIpAddressHelper.getIp()); // USAR setIpAddressLocal
			entityUser.setRole(UserRole.RESIDENTE);
			entityUser.setActive(true);
			entityUser.setFirstLogin(true);
			entityUser.setTemporalPassword(temporalPassword);

			repositoryUser.save(entityUser);
			response.setTemporalPassword(temporalPassword);

			try {
				emailHelper.sendTemporaryCredentials(request.getEmail(), request.getEmail(), temporalPassword);
			} catch (Exception e) {
				System.err.println("Error al enviar email: " + e.getMessage());
			}

			response.setType("success");
			response.getListMessage().add("Usuario registrado exitosamente. Se enviaron las credenciales a su email.");

			return response;

		} catch (Exception e) {
			response.setType("error");
			response.getListMessage().add("Error al registrar usuario: " + e.getMessage());
			return response;
		}
	}

	public ResponseUserInsert resetPassword(String idUser) {
		ResponseUserInsert response = new ResponseUserInsert();

		Optional<EntityUser> optional = repositoryUser.findById(idUser);

		if (!optional.isPresent()) {
			response.setType("error");
			response.getListMessage().add("Usuario no encontrado");
			return response;
		}

		EntityUser user = optional.get();
		String nuevaTemporalPassword = generateTemporalPassword();

		user.setPassword(passwordEncoder().encode(nuevaTemporalPassword));
		user.setFirstLogin(true);
		user.setTemporalPassword(nuevaTemporalPassword);

		repositoryUser.save(user);

		try {
			emailHelper.sendTemporaryCredentials(
					user.getEmail(),
					user.getEmail(),
					nuevaTemporalPassword);
		} catch (Exception e) {
			System.err.println("Error enviando email: " + e.getMessage());
		}

		response.setType("success");
		response.setTemporalPassword(nuevaTemporalPassword);
		response.getListMessage().add("Contraseña restablecida y enviada por email");

		return response;
	}

	// CAMBIAR CONTRASEÑA
	public ResponseUserUpdatePassword changePassword(String userId, RequestChangePassword request) {
		ResponseUserUpdatePassword response = new ResponseUserUpdatePassword();

		Optional<EntityUser> optional = repositoryUser.findById(userId);

		if (!optional.isPresent()) {
			response.setType("error");
			response.getListMessage().add("Usuario no encontrado");
			return response;
		}

		EntityUser user = optional.get();

		if (!passwordEncoder().matches(request.getOldPassword(), user.getPassword())) {
			response.setType("error");
			response.getListMessage().add("Contraseña actual incorrecta");
			return response;
		}

		user.setPassword(passwordEncoder().encode(request.getNewPassword()));
		user.setFirstLogin(false);
		user.setTemporalPassword(null);
		repositoryUser.save(user);

		response.setType("success");
		response.getListMessage().add("Contraseña actualizada exitosamente");

		return response;
	}

	// DESACTIVAR USUARIO
	public ResponseUserUpdate deactivateUser(String userId, RequestDeactivateUser request) {
		ResponseUserUpdate response = new ResponseUserUpdate();

		Optional<EntityUser> optional = repositoryUser.findById(userId);

		if (optional.isPresent()) {
			EntityUser user = optional.get();
			user.setActive(request.getActive());
			repositoryUser.save(user);

			response.setType("success");
			response.getListMessage().add(request.getActive() ? "Usuario activado" : "Usuario desactivado");
			return response;
		}

		response.setType("error");
		response.getListMessage().add("Usuario no encontrado");
		return response;
	}

	// OBTENER PERFIL PROPIO
	public Map<String, Object> getMyProfile(String userId) {
		Map<String, Object> res = new HashMap<>();
		ResponseUserGetById response = new ResponseUserGetById();

		Optional<EntityUser> entityUser = repositoryUser.findById(userId);

		if (entityUser.isPresent()) {
			response.setType("success");
			response.getListMessage().add("Perfil obtenido correctamente");
			res.put("message", response);
			res.put("data", entityUser.get());
		} else {
			response.setType("error");
			response.getListMessage().add("Usuario no encontrado");
			res.put("message", response);
			res.put("data", null);
		}

		return res;
	}

	// ACTUALIZAR PERFIL PROPIO
	public ResponseUserUpdate updateMyProfile(String userId, RequestUserUpdate request) {
		ResponseUserUpdate response = new ResponseUserUpdate();

		Optional<EntityUser> optional = repositoryUser.findById(userId);

		if (optional.isPresent()) {
			EntityUser entityUser = optional.get();

			entityUser.setFirstName(request.getFirstName());
			entityUser.setSurName(request.getSurName());
			entityUser.setCellPhoneNumber(String.valueOf(request.getCellPhoneNumber()));
			entityUser.setCellPhoneEmergency(String.valueOf(request.getCellPhoneEmergency()));

			repositoryUser.save(entityUser);

			response.setType("success");
			response.getListMessage().add("Perfil actualizado correctamente");
			return response;
		}

		response.setType("error");
		response.getListMessage().add("Usuario no encontrado");
		return response;
	}

	// MÉTODOS EXISTENTES
	public Map<String, Object> getAll() {
		ResponseUserGetAll response = new ResponseUserGetAll();
		Map<String, Object> res = new HashMap<>();
		List<EntityUser> entityUser = repositoryUser.findAll();

		response.setType("success");
		response.getListMessage().add("Usuarios extraídos correctamente");

		res.put("message", response);
		res.put("data", entityUser);

		return res;
	}

	public Map<String, Object> getById(String idUser) {
		ResponseUserGetById response = new ResponseUserGetById();
		Map<String, Object> res = new HashMap<>();
		Optional<EntityUser> entityUser = repositoryUser.findById(idUser);

		response.setType("success");
		response.getListMessage().add("Usuario extraído correctamente");

		res.put("message", response);
		res.put("data", entityUser);

		return res;
	}

	public ResponseUserDeleteById deleteById(String idUser) {
		ResponseUserDeleteById response = new ResponseUserDeleteById();
		repositoryUser.deleteById(idUser);

		response.setType("success");
		response.getListMessage().add("Usuario eliminado correctamente");

		return response;
	}

	// MÉTODO update EXISTENTE (renombrado para no confundir)
	public ResponseUserUpdate updateUser(String idUser, RequestUserUpdate request) {
		ResponseUserUpdate response = new ResponseUserUpdate();

		Optional<EntityUser> optional = repositoryUser.findById(idUser);

		if (optional.isPresent()) {
			EntityUser entityUser = optional.get();

			EntityResidence entityResidence = new EntityResidence();
			entityResidence.setIdResidence(request.getIdResidence());

			entityUser.setParentResidence(entityResidence);
			entityUser.setFirstName(request.getFirstName());
			entityUser.setSurName(request.getSurName());
			entityUser.setEmail(request.getEmail());
			entityUser.setCellPhoneNumber(String.valueOf(request.getCellPhoneNumber()));
			entityUser.setCellPhoneEmergency(String.valueOf(request.getCellPhoneEmergency()));
			entityUser.setIpAddressLocal(obtainIpAddressHelper.getIp());

			repositoryUser.save(entityUser);

			response.setType("success");
			response.getListMessage().add("Usuario actualizado correctamente");

			return response;
		}

		response.setType("error");
		response.getListMessage().add("Error el Usuario no se Actualizo");

		return response;
	}

	// MÉTODO updatePassword EXISTENTE
	public ResponseUserUpdatePassword updatePasswordUser(String email, RequestUserUpdatePassword request) {
		ResponseUserUpdatePassword response = new ResponseUserUpdatePassword();

		Optional<EntityUser> optional = repositoryUser.findByEmail(email);

		if (optional.isPresent()) {
			EntityUser entityUser = optional.get();

			entityUser.setPassword(passwordEncoder().encode(request.getPassword()));

			repositoryUser.save(entityUser);

			response.setType("success");
			response.getListMessage().add("Contraseña actualizada correctamente");
			return response;
		}

		response.setType("error");
		response.getListMessage().add("Error la Contraseña no se actualizo");

		return response;
	}

	private String generateTemporalPassword() {
		String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$";
		StringBuilder password = new StringBuilder();
		for (int i = 0; i < 10; i++) {
			int index = (int) (Math.random() * chars.length());
			password.append(chars.charAt(index));
		}
		return password.toString();
	}
}
