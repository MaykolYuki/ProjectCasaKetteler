package com.epiis.projectcasaketteler.dto.response;

import com.epiis.projectcasaketteler.entity.EntityAttendance;
import java.util.Date;

public class ResponseAttendanceDTO {
    private String idAtendance;
    private String idUser;
    private String firstName;
    private String surName;
    private Date eventTimestamp;
    private String eventType;
    private Boolean esAnomalia;
    private String motivoFallo;
    private String description;
    private String ssid;
    private String bssid;
    private Double serverSimilarity;
    private Date created_at;

    public ResponseAttendanceDTO(EntityAttendance a) {
        this.idAtendance = a.getIdAtendance();
        this.eventTimestamp = a.getEventTimestamp();
        this.eventType = a.getEventType() != null ? a.getEventType().name() : null;
        this.esAnomalia = a.getEsAnomalia();
        this.motivoFallo = a.getMotivoFallo();
        this.description = a.getDescription();
        this.ssid = a.getSsid();
        this.bssid = a.getBssid();
        this.serverSimilarity = a.getServerSimilarity();
        this.created_at = a.getCreated_at();

        if (a.getParentUser() != null) {
            this.idUser = a.getParentUser().getIdUser();
            this.firstName = a.getParentUser().getFirstName();
            this.surName = a.getParentUser().getSurName();
        }
    }

    public String getIdAtendance() {
        return idAtendance;
    }

    public String getIdUser() {
        return idUser;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getSurName() {
        return surName;
    }

    public Date getEventTimestamp() {
        return eventTimestamp;
    }

    public String getEventType() {
        return eventType;
    }

    public Boolean getEsAnomalia() {
        return esAnomalia;
    }

    public String getMotivoFallo() {
        return motivoFallo;
    }

    public String getDescription() {
        return description;
    }

    public String getSsid() {
        return ssid;
    }

    public String getBssid() {
        return bssid;
    }

    public Double getServerSimilarity() {
        return serverSimilarity;
    }

    public Date getCreated_at() {
        return created_at;
    }
}