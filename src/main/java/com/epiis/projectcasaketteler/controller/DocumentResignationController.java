package com.epiis.projectcasaketteler.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.epiis.projectcasaketteler.business.BusinessDocumentResignation;
import com.epiis.projectcasaketteler.dto.request.RequestDocumentResignationInsert;
import com.epiis.projectcasaketteler.dto.response.ResponseDocumentResignationInsert;
import com.epiis.projectcasaketteler.helper.JwtHelper;

@RestController
@RequestMapping(path = "casaketteler")
public class DocumentResignationController {
	@Autowired
	BusinessDocumentResignation businessDocumentResignation;

	@Autowired
	JwtHelper jwtHelper;

	@PostMapping(path = "registerdocumentresignation")
	public ResponseEntity<ResponseDocumentResignationInsert> insert(
			@ModelAttribute RequestDocumentResignationInsert request) throws Exception {
		ResponseDocumentResignationInsert response = businessDocumentResignation.insert(request);

		return ResponseEntity.ok(response);
	}

	// Admin lista renuncias de un residente
	@GetMapping(path = "resignation/{idUser}")
	public ResponseEntity<Map<String, Object>> getByUser(@PathVariable String idUser) {
		return ResponseEntity.ok(businessDocumentResignation.getByUser(idUser));
	}

	// Residente ve su propia renuncia
	@GetMapping(path = "myresignation")
	public ResponseEntity<Map<String, Object>> getMyResignation(
			@RequestHeader("Authorization") String token) {
		String userId = jwtHelper.extractUserId(token.substring(7));
		return ResponseEntity.ok(businessDocumentResignation.getMyResignation(userId));
	}

	// Admin actualiza estado y observaciones
	@PatchMapping(path = "resignation/{idDocument}/status")
	public ResponseEntity<ResponseDocumentResignationInsert> updateStatus(
			@PathVariable String idDocument,
			@RequestParam String status,
			@RequestParam(required = false) String observations) {
		return ResponseEntity.ok(
				businessDocumentResignation.updateStatus(idDocument, status, observations));
	}

	@GetMapping(path = "resignation/download/{idDocument}")
	public ResponseEntity<org.springframework.core.io.Resource> download(
			@PathVariable String idDocument) throws Exception {
		org.springframework.core.io.Resource resource = businessDocumentResignation.download(idDocument);
		return ResponseEntity.ok()
				.header("Content-Disposition",
						"attachment; filename=\"" + resource.getFilename() + "\"")
				.body(resource);
	}
}
