package com.epiis.projectcasaketteler.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
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

import com.epiis.projectcasaketteler.business.BusinessDocumentEnter;
import com.epiis.projectcasaketteler.dto.request.RequestDocumentEnterInsert;
import com.epiis.projectcasaketteler.dto.response.ResponseDocumentEnterInsert;
import com.epiis.projectcasaketteler.helper.JwtHelper;

@RestController
@RequestMapping(path = "casaketteler")
public class DocumentEnterController {
	@Autowired
	private BusinessDocumentEnter businessDocumentEnter;

	@Autowired
	private JwtHelper jwtHelper;

	@PostMapping(path = "registerdocumententer", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ResponseDocumentEnterInsert> insert(@ModelAttribute RequestDocumentEnterInsert request)
			throws Exception {
		ResponseDocumentEnterInsert response = businessDocumentEnter.insert(request);
		return ResponseEntity.ok(response);
	}

	// Admin lista los documentos de entrada de un residente
	@GetMapping(path = "documententer/{idUser}")
	public ResponseEntity<Map<String, Object>> getByUser(@PathVariable String idUser) {
		return ResponseEntity.ok(businessDocumentEnter.getByUser(idUser));
	}

	// Residente ve sus documentos de entrada descargables
	@GetMapping(path = "mydocumententer")
	public ResponseEntity<Map<String, Object>> getMyDocuments(
			@RequestHeader("Authorization") String token) {
		String userId = jwtHelper.extractUserId(token.substring(7));
		return ResponseEntity.ok(businessDocumentEnter.getMyDownloadableDocuments(userId));
	}

	// Admin actualiza estado y observaciones
	@PatchMapping(path = "documententer/{idDocument}/status")
	public ResponseEntity<ResponseDocumentEnterInsert> updateStatus(
			@PathVariable String idDocument,
			@RequestParam String status,
			@RequestParam(required = false) String observations) {
		return ResponseEntity.ok(
				businessDocumentEnter.updateStatus(idDocument, status, observations));
	}

	@GetMapping(path = "documententer/download/{idDocument}")
	public ResponseEntity<org.springframework.core.io.Resource> download(
			@PathVariable String idDocument,
			@RequestHeader("Authorization") String token) throws Exception {
		String requesterId = jwtHelper.extractUserId(token.substring(7));
		String requesterRole = jwtHelper.extractRole(token.substring(7));
		org.springframework.core.io.Resource resource = businessDocumentEnter.download(idDocument, requesterId,
				requesterRole);
		return ResponseEntity.ok()
				.header("Content-Disposition",
						"attachment; filename=\"" + resource.getFilename() + "\"")
				.body(resource);
	}

	@GetMapping(path = "mydocumententer/all")
	public ResponseEntity<Map<String, Object>> getMyAllDocuments(
			@RequestHeader("Authorization") String token) {
		String userId = jwtHelper.extractUserId(token.substring(7));
		return ResponseEntity.ok(businessDocumentEnter.getByUser(userId));
	}
}