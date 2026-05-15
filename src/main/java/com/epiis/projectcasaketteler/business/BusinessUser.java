package com.epiis.projectcasaketteler.business;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.epiis.projectcasaketteler.dto.request.RequestUserInsert;
import com.epiis.projectcasaketteler.dto.request.RequestUserUpdate;
import com.epiis.projectcasaketteler.dto.request.RequestUserUpdatePassword;
import com.epiis.projectcasaketteler.dto.response.ResponseUserDeleteById;
import com.epiis.projectcasaketteler.dto.response.ResponseUserGetAll;
import com.epiis.projectcasaketteler.dto.response.ResponseUserGetById;
import com.epiis.projectcasaketteler.dto.response.ResponseUserInsert;
import com.epiis.projectcasaketteler.dto.response.ResponseUserUpdate;
import com.epiis.projectcasaketteler.dto.response.ResponseUserUpdatePassword;
import com.epiis.projectcasaketteler.entity.EntityResidence;
import com.epiis.projectcasaketteler.entity.EntityUser;
import com.epiis.projectcasaketteler.helper.ObtainIpAddressHelper;
import com.epiis.projectcasaketteler.helper.PasswordEncoderHelper;
import com.epiis.projectcasaketteler.repository.RepositoryUser;

@Service
public class BusinessUser {
	@Autowired
	RepositoryUser repositoryUser;
	
	@Autowired
	PasswordEncoderHelper passwordEncoderHelper;
	
	@Autowired
	ObtainIpAddressHelper obtainIpAddressHelper;
	
	public ResponseUserInsert insert(RequestUserInsert request) {
		ResponseUserInsert response = new ResponseUserInsert();
		
		EntityUser entityUser = new EntityUser();
		
		EntityResidence entityResidence = new EntityResidence();
		
		entityResidence.setIdResidence(request.getIdResidence());
		
		entityUser.setIdUser(UUID.randomUUID().toString());
		entityUser.setParentResidence(entityResidence);
		entityUser.setFirstName(request.getFirstName());
		entityUser.setSurName(request.getSurName());
		entityUser.setEmail(request.getEmail());
		entityUser.setPassword(passwordEncoderHelper.passwordEncoder().encode(request.getPassword()));
		entityUser.setCellPhoneNumber(request.getCellPhoneNumber());
		entityUser.setCellPhoneEmergency(request.getCellPhoneEmergency());
		entityUser.setIdAddressLocal(obtainIpAddressHelper.getIp());
		entityUser.setCreated_at(new java.sql.Date(new Date().getTime()));
		entityUser.setUpdated_at(entityResidence.getCreated_at());
		
		repositoryUser.save(entityUser);
		
		response.success();
		response.getListMessage().add("Usuario Registrado Exitosamente");
		
		return response;
	}
	
	public Map<String, Object> getAll() {
		ResponseUserGetAll response = new ResponseUserGetAll();
		
		Map<String, Object> res = new HashMap<>();
		
		List<EntityUser> entityUser = repositoryUser.findAll();
		
		response.success();
		response.getListMessage().add("Usuarios Extraidos Correctamente");
		
		res.put("message", response);
		res.put("data", entityUser);
		
		return res;
	}
	
	public Map<String, Object> getById(String idUser) {
		ResponseUserGetById response = new ResponseUserGetById();
		
		Map<String, Object> res = new HashMap<>();
		
		Optional<EntityUser> entityUser = repositoryUser.findById(idUser);
		
		response.success();
		response.getListMessage().add("Usuario Extraido Correctamente");
		
		res.put("message", response);
		res.put("data", entityUser);
		
		return res;
	}
	
	public ResponseUserDeleteById deleteById(String idUser) {
		ResponseUserDeleteById response = new ResponseUserDeleteById();
		
		repositoryUser.deleteById(idUser);
		
		response.success();
		response.getListMessage().add("Usuario eliminado correctamente");
		
		return response;
	}
	
	public ResponseUserUpdate update(String idUser, RequestUserUpdate request) {
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
			entityUser.setCellPhoneNumber(request.getCellPhoneNumber());
			entityUser.setCellPhoneEmergency(request.getCellPhoneEmergency());
			entityUser.setIdAddressLocal(obtainIpAddressHelper.getIp());
			entityUser.setUpdated_at(entityResidence.getCreated_at());
			
			repositoryUser.save(entityUser);
			
			response.success();
			response.getListMessage().add("Usuario Actualizado Correctamente");
			
			return response;
		}
		
		response.error();
		response.getListMessage().add("Error el Usario no se Actualizo");
		
		return response;
	}
	
	public ResponseUserUpdatePassword updatePassword(String email, RequestUserUpdatePassword request) {
		ResponseUserUpdatePassword response = new ResponseUserUpdatePassword();
		
		Optional<EntityUser> optional = repositoryUser.findByEmail(email);
		
		if (optional.isPresent()) {
			EntityUser entityUser = optional.get();
			
			entityUser.setPassword(passwordEncoderHelper.passwordEncoder().encode(request.getPassword()));
			entityUser.setUpdated_at(new java.sql.Date(new Date().getTime()));
			
			repositoryUser.save(entityUser);
			
			response.success();
			response.getListMessage().add("Contraseña Actualizada Correctamente");
		}
		
		response.error();
		response.getListMessage().add("Error la Contraseña no se actualizo");
		
		return response;
	}
}
