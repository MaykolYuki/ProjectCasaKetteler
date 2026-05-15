package com.epiis.projectcasaketteler.dto.request;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RequestAdminInsert {
	private String idResidence;
	private String firstName;
	private String surName;
	private String email;
	private String password;
}
