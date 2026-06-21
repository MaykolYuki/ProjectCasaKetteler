package com.epiis.projectcasaketteler.dto.request;

import org.springframework.web.multipart.MultipartFile;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RequestAttendanceInsert {
	private String idUser;
	private String description;
	private MultipartFile file;
	private String ssid;
	private String bssid;
}
