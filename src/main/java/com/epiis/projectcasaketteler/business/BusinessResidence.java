package com.epiis.projectcasaketteler.business;

import java.net.InetAddress;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.epiis.projectcasaketteler.dto.request.RequestResidenceInsert;
import com.epiis.projectcasaketteler.dto.request.RequestResidenceUpdate;
import com.epiis.projectcasaketteler.dto.response.ResponseResidenceDeleteById;
import com.epiis.projectcasaketteler.dto.response.ResponseResidenceGetById;
import com.epiis.projectcasaketteler.dto.response.ResponseResidenceInsert;
import com.epiis.projectcasaketteler.dto.response.ResponseResidenceUpdate;
import com.epiis.projectcasaketteler.dto.response.ResponseResidenveGetAll;
import com.epiis.projectcasaketteler.entity.EntityResidence;
import com.epiis.projectcasaketteler.repository.RepositoryResidence;

@Service
public class BusinessResidence {
	@Autowired
	private RepositoryResidence repositoryResidence;

	public ResponseResidenceInsert insert(RequestResidenceInsert request) throws Exception {
		ResponseResidenceInsert response = new ResponseResidenceInsert();

		EntityResidence entityResidence = new EntityResidence();

		entityResidence.setIdResidence(UUID.randomUUID().toString());
		entityResidence.setName(request.getName());
		entityResidence.setIpAddress(InetAddress.getLocalHost().toString());
		entityResidence.setCreated_at(new java.sql.Date(new Date().getTime()));
		entityResidence.setUpdated_at(entityResidence.getCreated_at());
		entityResidence.setWifiSsid(request.getWifiSsid());
		entityResidence.setWifiBssid(request.getWifiBssid());

		repositoryResidence.save(entityResidence);

		response.success();
		response.getListMessage().add("Residencia Registrada Correctamente");

		return response;
	}

	public Map<String, Object> getAll() {
		ResponseResidenveGetAll response = new ResponseResidenveGetAll();

		Map<String, Object> res = new HashMap<>();

		List<EntityResidence> entityResidence = repositoryResidence.findAll();

		response.success();
		response.getListMessage().add("Residencias Extraidas Correctamente");

		res.put("message", response);
		res.put("data", entityResidence);

		return res;
	}

	public Map<String, Object> getById(String idResidence) {
		ResponseResidenceGetById response = new ResponseResidenceGetById();

		Map<String, Object> res = new HashMap<>();

		Optional<EntityResidence> entityResidence = repositoryResidence.findById(idResidence);

		response.success();
		response.getListMessage().add("Residencia Extraida Correctamente");

		res.put("message", response);
		res.put("data", entityResidence);

		return res;
	}

	public ResponseResidenceDeleteById deleteById(String idResidence) {
		ResponseResidenceDeleteById response = new ResponseResidenceDeleteById();

		repositoryResidence.deleteById(idResidence);

		response.success();
		response.getListMessage().add("Residencia Eliminada correctamente");

		return response;
	}

	public ResponseResidenceUpdate update(String idResidence, RequestResidenceUpdate request) throws Exception {
		ResponseResidenceUpdate response = new ResponseResidenceUpdate();

		Optional<EntityResidence> optional = repositoryResidence.findById(idResidence);

		if (optional.isPresent()) {
			EntityResidence entityResidence = optional.get();

			entityResidence.setName(request.getName());
			entityResidence.setIpAddress(InetAddress.getLocalHost().toString());
			entityResidence.setUpdated_at(new java.sql.Date(new Date().getTime()));
			entityResidence.setWifiSsid(request.getWifiSsid());
			entityResidence.setWifiBssid(request.getWifiBssid());

			repositoryResidence.save(entityResidence);

			response.success();
			response.getListMessage().add("Residencia Actualizada Correctamente");

			return response;
		}
		response.error();
		response.getListMessage().add("Error la Residencia no se Actualizo");

		return response;
	}
}
