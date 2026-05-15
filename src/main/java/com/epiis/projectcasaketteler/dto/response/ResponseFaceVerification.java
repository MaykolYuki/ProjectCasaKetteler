package com.epiis.projectcasaketteler.dto.response;

import com.epiis.projectcasaketteler.generic.ResponseGeneric;

public class ResponseFaceVerification extends ResponseGeneric{
	private boolean verified;
	private String error;
	
	public boolean isVerified() {
		return verified;
	}
	
	public void setVerified(boolean verified) {
		this.verified = verified;
	}
	
	public String getError() {
		return error;
	}
	
	public void setError(String error) {
		this.error = error;
	}
}
