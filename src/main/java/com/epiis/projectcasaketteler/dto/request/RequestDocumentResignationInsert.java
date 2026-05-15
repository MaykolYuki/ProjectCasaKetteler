package com.epiis.projectcasaketteler.dto.request;

import org.springframework.web.multipart.MultipartFile;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RequestDocumentResignationInsert {
	private String idUser;
	private MultipartFile file;
}
