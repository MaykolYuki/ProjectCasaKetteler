package com.epiis.projectcasaketteler.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.epiis.projectcasaketteler.business.BusinessDocumentGeneral;
import com.epiis.projectcasaketteler.dto.request.RequestDocumentGeneralInsert;
import com.epiis.projectcasaketteler.dto.response.ResponseDocumentGeneralInsert;

@RestController
@RequestMapping(path = "casaketteler")
public class DocumentGeneralController {
	@Autowired
	BusinessDocumentGeneral businessDocumentGeneral;
	
	@PostMapping(path = "registerdocumentgeneral", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ResponseDocumentGeneralInsert> insert(@ModelAttribute RequestDocumentGeneralInsert request) throws Exception{
		ResponseDocumentGeneralInsert response = businessDocumentGeneral.insert(request);
		
		return ResponseEntity.ok(response);
	}
}
