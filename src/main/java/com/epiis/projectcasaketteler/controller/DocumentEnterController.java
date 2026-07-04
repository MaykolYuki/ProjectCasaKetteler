package com.epiis.projectcasaketteler.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.epiis.projectcasaketteler.business.BusinessDocumentEnter;
import com.epiis.projectcasaketteler.dto.request.RequestDocumentEnterInsert;
import com.epiis.projectcasaketteler.dto.response.ResponseDocumentEnterInsert;

@RestController
@RequestMapping(path = "casaketteler")
public class DocumentEnterController {
	@Autowired
	private BusinessDocumentEnter businessDocumentEnter;

	@PostMapping(path = "registerdocumententer", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ResponseDocumentEnterInsert> insert(@ModelAttribute RequestDocumentEnterInsert request)
			throws Exception {
		ResponseDocumentEnterInsert response = businessDocumentEnter.insert(request);

		return ResponseEntity.ok(response);
	}
}
