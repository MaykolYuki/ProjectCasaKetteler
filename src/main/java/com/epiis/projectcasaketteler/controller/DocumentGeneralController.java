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

import com.epiis.projectcasaketteler.business.BusinessDocumentGeneral;
import com.epiis.projectcasaketteler.dto.request.RequestDocumentGeneralInsert;
import com.epiis.projectcasaketteler.dto.response.ResponseDocumentGeneralInsert;
import com.epiis.projectcasaketteler.helper.JwtHelper;

@RestController
@RequestMapping(path = "casaketteler")
public class DocumentGeneralController {
	@Autowired
	BusinessDocumentGeneral businessDocumentGeneral;

	@Autowired
	JwtHelper jwtHelper;

	@PostMapping(path = "registerdocumentgeneral", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ResponseDocumentGeneralInsert> insert(@ModelAttribute RequestDocumentGeneralInsert request)
			throws Exception {
		ResponseDocumentGeneralInsert response = businessDocumentGeneral.insert(request);

		return ResponseEntity.ok(response);
	}

	// Admin lista documentos de un residente, con filtro opcional por tipo
	@GetMapping(path = "documents/{idUser}")
	public ResponseEntity<Map<String, Object>> getByUser(
			@PathVariable String idUser,
			@RequestParam(required = false) String type) {
		return ResponseEntity.ok(businessDocumentGeneral.getByUser(idUser, type));
	}

	// Residente ve sus documentos descargables
	@GetMapping(path = "mydocuments")
	public ResponseEntity<Map<String, Object>> getMyDocuments(
			@RequestHeader("Authorization") String token) {
		String userId = jwtHelper.extractUserId(token.substring(7));
		return ResponseEntity.ok(businessDocumentGeneral.getMyDownloadableDocuments(userId));
	}

	// Admin actualiza estado y observaciones
	@PatchMapping(path = "documents/{idDocument}/status")
	public ResponseEntity<ResponseDocumentGeneralInsert> updateStatus(
			@PathVariable String idDocument,
			@RequestParam String status,
			@RequestParam(required = false) String observations) {
		return ResponseEntity.ok(
				businessDocumentGeneral.updateStatus(idDocument, status, observations));
	}

	@GetMapping(path = "documents/download/{idDocument}")
	public ResponseEntity<org.springframework.core.io.Resource> download(
			@PathVariable String idDocument) throws Exception {
		org.springframework.core.io.Resource resource = businessDocumentGeneral.download(idDocument);
		return ResponseEntity.ok()
				.header("Content-Disposition",
						"attachment; filename=\"" + resource.getFilename() + "\"")
				.body(resource);
	}
}
