package com.epiis.projectcasaketteler.generic;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ResponseGeneric {
	private String type;
	private final List<String> listMessage;
	
	protected ResponseGeneric() {
		this.type = "error";
		this.listMessage = new ArrayList<>();
	}
	
	public String getType() {
		return this.type;
	}
	
	public void success() {
		this.type = "success";
	}
	
	public void warning() {
		this.type = "warning";
	}
	
	public void error() {
		this.type = "error";
	}
	
	public void exception() {
		this.type = "exception";
	}
}
