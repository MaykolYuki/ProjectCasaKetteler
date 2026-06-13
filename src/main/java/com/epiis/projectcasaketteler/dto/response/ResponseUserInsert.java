package com.epiis.projectcasaketteler.dto.response;

import com.epiis.projectcasaketteler.generic.ResponseGeneric;

public class ResponseUserInsert extends ResponseGeneric {

    private String temporalPassword;

    public String getTemporalPassword() {
        return temporalPassword;
    }

    public void setTemporalPassword(String temporalPassword) {
        this.temporalPassword = temporalPassword;
    }
}
