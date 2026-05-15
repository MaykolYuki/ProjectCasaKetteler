package com.epiis.projectcasaketteler.dto.request;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RequestUserUpdate {
	private String idResidence;
	private String firstName;
	private String surName;
	private String email;
	private String password;
	private int cellPhoneNumber;
	private int cellPhoneEmergency;
}
