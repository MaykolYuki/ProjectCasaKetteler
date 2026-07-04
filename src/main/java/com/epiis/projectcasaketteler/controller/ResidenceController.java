package com.epiis.projectcasaketteler.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.epiis.projectcasaketteler.business.BusinessResidence;
import com.epiis.projectcasaketteler.dto.request.RequestResidenceInsert;
import com.epiis.projectcasaketteler.dto.request.RequestResidenceUpdate;
import com.epiis.projectcasaketteler.dto.response.ResponseResidenceDeleteById;
import com.epiis.projectcasaketteler.dto.response.ResponseResidenceInsert;
import com.epiis.projectcasaketteler.dto.response.ResponseResidenceUpdate;

@RestController
@RequestMapping(path = "casaketteler")
public class ResidenceController {
	@Autowired
	private BusinessResidence businessResidence;

	@PostMapping(path = "registerresidence", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseResidenceInsert> insert(@RequestBody RequestResidenceInsert request)
			throws Exception {
		ResponseResidenceInsert response = businessResidence.insert(request);

		return ResponseEntity.ok(response);
	}

	@GetMapping(path = "indexresidence")
	public ResponseEntity<Map<String, Object>> getAll() {

		return ResponseEntity.ok(businessResidence.getAll());
	}

	@GetMapping(path = "showresidence/{idResidence}")
	public ResponseEntity<Map<String, Object>> getById(@PathVariable String idResidence) {

		return ResponseEntity.ok(businessResidence.getById(idResidence));
	}

	@DeleteMapping(path = "deleteresidence/{idResidence}")
	public ResponseEntity<ResponseResidenceDeleteById> deleteById(@PathVariable String idResidence) {
		ResponseResidenceDeleteById response = businessResidence.deleteById(idResidence);

		return ResponseEntity.ok(response);
	}

	@PutMapping(path = "updateresidence/{idResidence}", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseResidenceUpdate> update(@PathVariable String idResidence,
			@RequestBody RequestResidenceUpdate request) throws Exception {
		ResponseResidenceUpdate response = businessResidence.update(idResidence, request);

		return ResponseEntity.ok(response);
	}
}
