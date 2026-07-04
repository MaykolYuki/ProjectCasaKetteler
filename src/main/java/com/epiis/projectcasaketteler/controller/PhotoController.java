package com.epiis.projectcasaketteler.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.epiis.projectcasaketteler.business.BusinessPhoto;
import com.epiis.projectcasaketteler.dto.request.RequestPhotoInsert;
import com.epiis.projectcasaketteler.dto.response.ResponsePhotoInsert;

@RestController
@RequestMapping(path = "casaketteler")
public class PhotoController {
	@Autowired
	private BusinessPhoto businessPhoto;

	@PostMapping(path = "registerphoto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ResponsePhotoInsert> insert(@ModelAttribute RequestPhotoInsert request) throws Exception {
		ResponsePhotoInsert response = businessPhoto.insert(request);

		return ResponseEntity.ok(response);
	}
}
