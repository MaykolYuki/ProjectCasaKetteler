package com.epiis.projectcasaketteler.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.epiis.projectcasaketteler.business.BusinessAttendance;
import com.epiis.projectcasaketteler.dto.request.RequestAttendanceInsert;
import com.epiis.projectcasaketteler.dto.response.ResponseFaceVerification;

@RestController
@RequestMapping(path = "casaketteler")
public class AttendanceController {
	@Autowired
    private BusinessAttendance businessAttendance;
	
	@PostMapping(path = "register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseFaceVerification> registerAttendance(@ModelAttribute RequestAttendanceInsert request) {
        ResponseFaceVerification response = businessAttendance.insert(request);
        
        boolean hasError = response.getListMessage().stream().anyMatch(msg -> msg.startsWith("Error"));
        
        if (hasError) {
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        
        return ResponseEntity.ok(response);
    }
}
