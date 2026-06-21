package com.epiis.projectcasaketteler.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Setter
@Getter
public class RequestAttendanceSync {
    private String idUser;
    private Date recordedAt; // timestamp real offline
    private Boolean isEntry; // true=entrada, false=salida
    private Double clientSimilarity;
    private String base64Image; // imagen para re-verificar con Python
    private String ssid; // SSID de la red Wi-Fi
    private String bssid; // BSSID de la red Wi-Fi
}