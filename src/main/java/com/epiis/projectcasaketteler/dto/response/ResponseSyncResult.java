package com.epiis.projectcasaketteler.dto.response;

import java.util.ArrayList;
import java.util.List;

public class ResponseSyncResult {
    private int procesados;
    private int rechazados;
    private String type;
    private List<SyncRecordDetail> detalle = new ArrayList<>();

    public static class SyncRecordDetail {
        private String idUser;
        private String recordedAt;
        private String resultado; // "PROCESADO" o "RECHAZADO"
        private String motivo; // null si procesado, mensaje si rechazado

        public SyncRecordDetail(String idUser, String recordedAt,
                String resultado, String motivo) {
            this.idUser = idUser;
            this.recordedAt = recordedAt;
            this.resultado = resultado;
            this.motivo = motivo;
        }

        public String getIdUser() {
            return idUser;
        }

        public String getRecordedAt() {
            return recordedAt;
        }

        public String getResultado() {
            return resultado;
        }

        public String getMotivo() {
            return motivo;
        }
    }

    public void agregarProcesado(String idUser, String recordedAt) {
        procesados++;
        detalle.add(new SyncRecordDetail(idUser, recordedAt, "PROCESADO", null));
    }

    public void agregarRechazado(String idUser, String recordedAt, String motivo) {
        rechazados++;
        detalle.add(new SyncRecordDetail(idUser, recordedAt, "RECHAZADO", motivo));
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public int getProcesados() {
        return procesados;
    }

    public int getRechazados() {
        return rechazados;
    }

    public List<SyncRecordDetail> getDetalle() {
        return detalle;
    }
}