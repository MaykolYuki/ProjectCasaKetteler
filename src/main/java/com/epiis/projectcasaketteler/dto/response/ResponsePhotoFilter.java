package com.epiis.projectcasaketteler.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ResponsePhotoFilter {
    private boolean success;

    @JsonProperty("best_image")
    private String bestImage;

    @JsonProperty("sharpness_score")
    private double sharpnessScore;

    private String error;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getBestImage() {
        return bestImage;
    }

    public void setBestImage(String bestImage) {
        this.bestImage = bestImage;
    }

    public double getSharpnessScore() {
        return sharpnessScore;
    }

    public void setSharpnessScore(double sharpnessScore) {
        this.sharpnessScore = sharpnessScore;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}