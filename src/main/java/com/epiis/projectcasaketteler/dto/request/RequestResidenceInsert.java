package com.epiis.projectcasaketteler.dto.request;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RequestResidenceInsert {
	private String name;
	private String wifiSsid;
	private String wifiBssid;
}
