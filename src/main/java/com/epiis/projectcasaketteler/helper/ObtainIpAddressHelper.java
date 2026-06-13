package com.epiis.projectcasaketteler.helper;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class ObtainIpAddressHelper {

	private HttpServletRequest getCurrentRequest() {
		ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		if (attributes != null) {
			return attributes.getRequest();
		}
		return null;
	}

	public String get(String campo) {
		HttpServletRequest request = getCurrentRequest();
		if (request != null) {
			return request.getParameter(campo);
		}
		return null;
	}

	public String getIp() {
		HttpServletRequest request = getCurrentRequest();
		if (request != null) {
			String ip = request.getHeader("X-Forwarded-For");
			if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
				ip = request.getHeader("Proxy-Client-IP");
			}
			if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
				ip = request.getHeader("WL-Proxy-Client-IP");
			}
			if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
				ip = request.getRemoteAddr();
			}
			return ip;
		}
		return "127.0.0.1";
	}
}