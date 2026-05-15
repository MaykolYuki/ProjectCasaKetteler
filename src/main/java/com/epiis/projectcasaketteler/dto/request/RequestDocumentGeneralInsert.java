package com.epiis.projectcasaketteler.dto.request;

import org.springframework.web.multipart.MultipartFile;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RequestDocumentGeneralInsert {
	private String idUser;
	private String type;
	private MultipartFile file;
}
