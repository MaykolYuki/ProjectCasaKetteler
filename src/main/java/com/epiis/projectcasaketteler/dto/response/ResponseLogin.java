package com.epiis.projectcasaketteler.dto.response;

import com.epiis.projectcasaketteler.generic.ResponseGeneric;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ResponseLogin extends ResponseGeneric {
    private String token;
    private String userId;
    private String role;
    private String firstName;
    private String surName;
    private Boolean firstLogin;
}