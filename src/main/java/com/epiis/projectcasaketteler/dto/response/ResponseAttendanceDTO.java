package com.epiis.projectcasaketteler.dto.response;

import com.epiis.projectcasaketteler.entity.EntityAttendance;
import java.util.Date;

public class ResponseAttendanceDTO {
    private String idAtendance;
    private String idUser;
    private String firstName;
    private String surName;
    private Date entryDate;
    private Date departureDate;
    private Boolean status;
    private Date created_at;
    private Date updated_at;
    private Double clientSimilarity;
    private Double serverSimilarity;
    private Boolean verifiedByServer;
    private Date recordedAt;
    private Date syncedAt;
    private String description;

    public ResponseAttendanceDTO(EntityAttendance a) {
        this.idAtendance = a.getIdAtendance();
        this.entryDate = a.getEntryDate();
        this.departureDate = a.getDepartureDate();
        this.status = a.getStatus();
        this.created_at = a.getCreated_at();
        this.updated_at = a.getUpdated_at();
        this.clientSimilarity = a.getClientSimilarity();
        this.serverSimilarity = a.getServerSimilarity();
        this.verifiedByServer = a.getVerifiedByServer();
        this.recordedAt = a.getRecordedAt();
        this.syncedAt = a.getSyncedAt();
        this.description = a.getDescription();

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

    public Date getEntryDate() {
        return entryDate;
    }

    public Date getDepartureDate() {
        return departureDate;
    }

    public Boolean getStatus() {
        return status;
    }

    public Date getCreated_at() {
        return created_at;
    }

    public Date getUpdated_at() {
        return updated_at;
    }

    public Double getClientSimilarity() {
        return clientSimilarity;
    }

    public Double getServerSimilarity() {
        return serverSimilarity;
    }

    public Boolean getVerifiedByServer() {
        return verifiedByServer;
    }

    public Date getRecordedAt() {
        return recordedAt;
    }

    public Date getSyncedAt() {
        return syncedAt;
    }

    public String getDescription() {
        return description;
    }
}