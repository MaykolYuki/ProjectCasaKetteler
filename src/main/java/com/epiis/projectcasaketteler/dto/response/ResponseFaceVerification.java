package com.epiis.projectcasaketteler.dto.response;

import com.epiis.projectcasaketteler.generic.ResponseGeneric;

public class ResponseFaceVerification extends ResponseGeneric {
	private boolean verified;
	private String error;
	private double similarity; // AGREGAR
	private Boolean entrada; // true = se registró entrada, false = se registró salida
	private Integer framesEvaluados;
	private Double distance;
	private Double threshold;

	public Integer getFramesEvaluados() {
		return framesEvaluados;
	}

	public void setFramesEvaluados(Integer framesEvaluados) {
		this.framesEvaluados = framesEvaluados;
	}

	public Double getDistance() {
		return distance;
	}

	public void setDistance(Double distance) {
		this.distance = distance;
	}

	public Double getThreshold() {
		return threshold;
	}

	public void setThreshold(Double threshold) {
		this.threshold = threshold;
	}

	public Boolean getEntrada() {
		return entrada;
	}

	public void setEntrada(Boolean entrada) {
		this.entrada = entrada;
	}

	public double getSimilarity() {
		return similarity;
	}

	public void setSimilarity(double similarity) {
		this.similarity = similarity;
	}

	// getters/setters existentes...
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
