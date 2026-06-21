package com.epiis.projectcasaketteler.dto.response;

import com.epiis.projectcasaketteler.generic.ResponseGeneric;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ResponsePhotoInsert extends ResponseGeneric {
    private List<Map<String, Object>> photoQualityReport = new ArrayList<>();

    public List<Map<String, Object>> getPhotoQualityReport() {
        return photoQualityReport;
    }

    public void setPhotoQualityReport(List<Map<String, Object>> report) {
        this.photoQualityReport = report;
    }
}