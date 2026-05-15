package com.epiis.projectcasaketteler.helper;

import jakarta.servlet.http.HttpServletRequest;

public class ObtainIpAddressHelper {
	private HttpServletRequest request;
	
	public ObtainIpAddressHelper(HttpServletRequest request) {
		this.request = request;
	}
	
	public String get(String campo) {
		return request.getParameter(campo);
	}
	
	public String getIp() {
		return request.getRemoteAddr();
	}
}
