package com.epiis.projectcasaketteler.dto.request;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RequestChangePassword {
    private String oldPassword;
    private String newPassword;
}