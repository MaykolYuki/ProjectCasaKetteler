package com.epiis.projectcasaketteler.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.epiis.projectcasaketteler.business.BusinessDocumentResignation;
import com.epiis.projectcasaketteler.dto.request.RequestDocumentResignationInsert;
import com.epiis.projectcasaketteler.dto.response.ResponseDocumentResignationInsert;

@RestController
@RequestMapping(path = "casaketteler")
public class DocumentResignationController {
	@Autowired
	BusinessDocumentResignation businessDocumentResignation;
	
	@PostMapping(path = "registerdocumentresignation")
	public ResponseEntity<ResponseDocumentResignationInsert> insert(@ModelAttribute RequestDocumentResignationInsert request) throws Exception{
		ResponseDocumentResignationInsert response = businessDocumentResignation.insert(request);
		
		return ResponseEntity.ok(response);
	}
}
