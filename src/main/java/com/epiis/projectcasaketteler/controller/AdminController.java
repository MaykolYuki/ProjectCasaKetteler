package com.epiis.projectcasaketteler.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.epiis.projectcasaketteler.business.BusinessAdmin;
import com.epiis.projectcasaketteler.dto.request.RequestAdminInsert;
import com.epiis.projectcasaketteler.dto.request.RequestAdminUpdate;
import com.epiis.projectcasaketteler.dto.request.RequestAdminUpdatePassword;
import com.epiis.projectcasaketteler.dto.request.RequestChangePassword;
import com.epiis.projectcasaketteler.dto.response.ResponseAdminDeleteById;
import com.epiis.projectcasaketteler.dto.response.ResponseAdminInsert;
import com.epiis.projectcasaketteler.dto.response.ResponseAdminUpdate;
import com.epiis.projectcasaketteler.dto.response.ResponseAdminUpdatePassword;
import com.epiis.projectcasaketteler.helper.JwtHelper;

@RestController
@RequestMapping(path = "casaketteler")
public class AdminController {
	@Autowired
	private BusinessAdmin businessAdmin;

	@Autowired
	private JwtHelper jwtHelper;

	@PostMapping(path = "registeradmin", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseAdminInsert> insert(@RequestBody RequestAdminInsert request) {
		ResponseAdminInsert response = businessAdmin.insert(request);

		return ResponseEntity.ok(response);
	}

	@GetMapping(path = "indexadmin")
	public ResponseEntity<Map<String, Object>> getAll() {

		return ResponseEntity.ok(businessAdmin.getAll());
	}

	@GetMapping(path = "showadmin/{idAdmin}")
	public ResponseEntity<Map<String, Object>> getById(@PathVariable String idAdmin) {

		return ResponseEntity.ok(businessAdmin.getById(idAdmin));
	}

	@DeleteMapping(path = "deleteadmin/{idAdmin}")
	public ResponseEntity<ResponseAdminDeleteById> deleteById(@PathVariable String idAdmin) {
		ResponseAdminDeleteById response = businessAdmin.deleteById(idAdmin);

		return ResponseEntity.ok(response);
	}

	@PutMapping(path = "updateadmin/{idAdmin}", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseAdminUpdate> update(@PathVariable String idAdmin,
			@RequestBody RequestAdminUpdate request) {
		ResponseAdminUpdate response = businessAdmin.update(idAdmin, request);

		return ResponseEntity.ok(response);
	}

	@PutMapping(path = "updatepasswordadmin/{email}")
	public ResponseEntity<ResponseAdminUpdatePassword> updatePassword(@PathVariable String email,
			@RequestBody RequestAdminUpdatePassword request) {
		ResponseAdminUpdatePassword response = businessAdmin.updatePassword(email, request);

		return ResponseEntity.ok(response);
	}

	@PutMapping(path = "changepasswordadmin", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseAdminUpdatePassword> changeMyPassword(
			@RequestHeader("Authorization") String token,
			@RequestBody RequestChangePassword request) {
		String adminId = jwtHelper.extractUserId(token.substring(7));
		ResponseAdminUpdatePassword response = businessAdmin.changeMyPassword(adminId, request);
		return ResponseEntity.ok(response);
	}
}
