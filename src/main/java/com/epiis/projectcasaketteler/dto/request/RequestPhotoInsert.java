package com.epiis.projectcasaketteler.dto.request;

import org.springframework.web.multipart.MultipartFile;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RequestPhotoInsert {
	private String idUser;
	private MultipartFile[] files;
}
