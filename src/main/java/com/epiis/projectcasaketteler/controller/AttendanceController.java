package com.epiis.projectcasaketteler.controller;

import java.util.Map;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.epiis.projectcasaketteler.business.BusinessAttendance;
import com.epiis.projectcasaketteler.dto.request.RequestAttendanceInsert;
import com.epiis.projectcasaketteler.dto.request.RequestAttendanceSync;
import com.epiis.projectcasaketteler.dto.response.ResponseFaceVerification;
import com.epiis.projectcasaketteler.helper.JwtHelper;

@RestController
@RequestMapping(path = "casaketteler")
public class AttendanceController {
    @Autowired
    private BusinessAttendance businessAttendance;

    @Autowired
    private JwtHelper jwtHelper;

    @PostMapping(path = "register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseFaceVerification> registerAttendance(
            @ModelAttribute RequestAttendanceInsert request) {
        ResponseFaceVerification response = businessAttendance.insert(request);

        boolean hasError = response.getListMessage().stream().anyMatch(msg -> msg.startsWith("Error"));

        if (hasError) {
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping(path = "myattendance")
    public ResponseEntity<Map<String, Object>> getMyAttendance(
            @RequestHeader("Authorization") String token) {
        String userId = jwtHelper.extractUserId(token.substring(7));
        return ResponseEntity.ok(businessAttendance.getByUser(userId));
    }

    @PostMapping(path = "attendance/sync", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> sync(
            @RequestHeader("Authorization") String token,
            @RequestBody List<RequestAttendanceSync> records) {
        String userId = jwtHelper.extractUserId(token.substring(7));
        return ResponseEntity.ok(businessAttendance.syncOfflineRecords(userId, records));
    }
}
