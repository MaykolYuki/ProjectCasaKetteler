package com.epiis.projectcasaketteler.business;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.epiis.projectcasaketteler.dto.request.RequestAdminInsert;
import com.epiis.projectcasaketteler.dto.request.RequestAdminUpdate;
import com.epiis.projectcasaketteler.dto.request.RequestAdminUpdatePassword;
import com.epiis.projectcasaketteler.dto.request.RequestChangePassword;
import com.epiis.projectcasaketteler.dto.response.ResponseAdminDeleteById;
import com.epiis.projectcasaketteler.dto.response.ResponseAdminGetAll;
import com.epiis.projectcasaketteler.dto.response.ResponseAdminGetById;
import com.epiis.projectcasaketteler.dto.response.ResponseAdminInsert;
import com.epiis.projectcasaketteler.dto.response.ResponseAdminUpdate;
import com.epiis.projectcasaketteler.dto.response.ResponseAdminUpdatePassword;
import com.epiis.projectcasaketteler.entity.EntityAdmin;
import com.epiis.projectcasaketteler.entity.EntityResidence;
import com.epiis.projectcasaketteler.helper.PasswordEncoderHelper;
import com.epiis.projectcasaketteler.repository.RepositoryAdmin;

@Service
public class BusinessAdmin {
	@Autowired
	private RepositoryAdmin repositoryAdmin;

	@Autowired
	private PasswordEncoderHelper passwordEncoderHelper;

	public ResponseAdminInsert insert(RequestAdminInsert request) {
		ResponseAdminInsert response = new ResponseAdminInsert();

		EntityAdmin entityAdmin = new EntityAdmin();
		EntityResidence entityResidence = new EntityResidence();

		entityResidence.setIdResidence(request.getIdResidence());

		entityAdmin.setIdAdmin(UUID.randomUUID().toString());
		entityAdmin.setParentResidence(entityResidence);
		entityAdmin.setFirstName(request.getFirstName());
		entityAdmin.setSurName(request.getSurName());
		entityAdmin.setEmail(request.getEmail());
		entityAdmin.setPassword(passwordEncoderHelper.passwordEncoder().encode(request.getPassword()));

		repositoryAdmin.save(entityAdmin);

		response.success();
		response.getListMessage().add("Admin Creado Correctamente");

		return response;
	}

	public Map<String, Object> getAll() {
		ResponseAdminGetAll response = new ResponseAdminGetAll();

		Map<String, Object> res = new HashMap<>();

		List<EntityAdmin> entityAdmin = repositoryAdmin.findAll();

		response.success();
		response.getListMessage().add("Admins Extraidos Correctamente");

		res.put("message", response);
		res.put("data", entityAdmin);

		return res;
	}

	public Map<String, Object> getById(String idAdmin) {
		ResponseAdminGetById response = new ResponseAdminGetById();

		Map<String, Object> res = new HashMap<>();

		Optional<EntityAdmin> entityAdmin = repositoryAdmin.findById(idAdmin);

		response.success();
		response.getListMessage().add("Admin Extraido Correctamente");

		res.put("message", response);
		res.put("data", entityAdmin);

		return res;
	}

	public ResponseAdminDeleteById deleteById(String idAdmin) {
		ResponseAdminDeleteById response = new ResponseAdminDeleteById();

		repositoryAdmin.deleteById(idAdmin);

		response.success();
		response.getListMessage().add("Admin Eliminado Correctamente");

		return response;
	}

	public ResponseAdminUpdate update(String idUpdate, RequestAdminUpdate request) {
		ResponseAdminUpdate response = new ResponseAdminUpdate();

		Optional<EntityAdmin> optional = repositoryAdmin.findById(idUpdate);

		if (optional.isPresent()) {
			EntityAdmin entityAdmin = optional.get();

			EntityResidence entityResidence = new EntityResidence();

			entityResidence.setIdResidence(request.getIdResidence());

			entityAdmin.setParentResidence(entityResidence);
			entityAdmin.setFirstName(request.getFirstName());
			entityAdmin.setSurName(request.getSurName());
			entityAdmin.setEmail(request.getEmail());
			entityAdmin.setUpdated_at(new java.sql.Date(new Date().getTime()));

			repositoryAdmin.save(entityAdmin);

			response.success();
			response.getListMessage().add("Admin Actualizado Correctamente");

			return response;
		}
		response.error();
		response.getListMessage().add("Error el Admin no se Actualizo");

		return response;
	}

	public ResponseAdminUpdatePassword updatePassword(String email, RequestAdminUpdatePassword request) {
		ResponseAdminUpdatePassword response = new ResponseAdminUpdatePassword();

		Optional<EntityAdmin> optional = repositoryAdmin.findByEmail(email);

		if (optional.isPresent()) {
			EntityAdmin entityAdmin = optional.get();

			entityAdmin.setPassword(passwordEncoderHelper.passwordEncoder().encode(request.getPassword()));
			entityAdmin.setTokenValidAfter(new Date());

			repositoryAdmin.save(entityAdmin);

			response.success();
			response.getListMessage().add("Contraseña Actualizada Correctamente");

			return response;
		}
		response.error();
		response.getListMessage().add("Error la contraseña no registrada");

		return response;
	}

	public ResponseAdminUpdatePassword changeMyPassword(String adminId, RequestChangePassword request) {
		ResponseAdminUpdatePassword response = new ResponseAdminUpdatePassword();

		Optional<EntityAdmin> optional = repositoryAdmin.findById(adminId);
		if (!optional.isPresent()) {
			response.error();
			response.getListMessage().add("Administrador no encontrado");
			return response;
		}

		EntityAdmin admin = optional.get();

		if (!passwordEncoderHelper.passwordEncoder().matches(request.getOldPassword(), admin.getPassword())) {
			response.error();
			response.getListMessage().add("Contraseña actual incorrecta");
			return response;
		}

		admin.setPassword(passwordEncoderHelper.passwordEncoder().encode(request.getNewPassword()));
		admin.setTokenValidAfter(new Date());
		repositoryAdmin.save(admin);

		response.success();
		response.getListMessage().add("Contraseña actualizada exitosamente");
		return response;
	}
}
