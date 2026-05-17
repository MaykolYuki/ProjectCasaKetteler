package com.epiis.projectcasaketteler.helper;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

@Component
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
