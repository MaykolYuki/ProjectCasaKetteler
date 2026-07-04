package com.epiis.projectcasaketteler.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.epiis.projectcasaketteler.business.BusinessUser;
import com.epiis.projectcasaketteler.dto.request.RequestChangePassword;
import com.epiis.projectcasaketteler.dto.request.RequestDeactivateUser;
import com.epiis.projectcasaketteler.dto.request.RequestLogin;
import com.epiis.projectcasaketteler.dto.request.RequestUserInsert;
import com.epiis.projectcasaketteler.dto.request.RequestUserUpdate;
import com.epiis.projectcasaketteler.dto.request.RequestUserUpdatePassword;
import com.epiis.projectcasaketteler.dto.response.ResponseLogin;
import com.epiis.projectcasaketteler.dto.response.ResponseUserDeleteById;
import com.epiis.projectcasaketteler.dto.response.ResponseUserInsert;
import com.epiis.projectcasaketteler.dto.response.ResponseUserUpdate;
import com.epiis.projectcasaketteler.dto.response.ResponseUserUpdatePassword;
import com.epiis.projectcasaketteler.helper.JwtHelper;

@RestController
@RequestMapping(path = "casaketteler")
public class UserController {
	@Autowired
	private BusinessUser businessUser;

	@Autowired
	private JwtHelper jwtHelper; // AGREGAR ESTO

	@PostMapping(path = "registeruser", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseUserInsert> insert(@RequestBody RequestUserInsert request) {
		ResponseUserInsert response = businessUser.insert(request);
		return ResponseEntity.ok(response);
	}

	@PostMapping(path = "login", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseLogin> login(@RequestBody RequestLogin request) {
		ResponseLogin response = businessUser.login(request);
		return ResponseEntity.ok(response);
	}

	@GetMapping(path = "myprofile")
	public ResponseEntity<Map<String, Object>> getMyProfile(@RequestHeader("Authorization") String token) {
		String userId = extractUserIdFromToken(token);
		return ResponseEntity.ok(businessUser.getMyProfile(userId));
	}

	@PutMapping(path = "myprofile", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseUserUpdate> updateMyProfile(
			@RequestHeader("Authorization") String token,
			@RequestBody RequestUserUpdate request) {
		String userId = extractUserIdFromToken(token);
		ResponseUserUpdate response = businessUser.updateMyProfile(userId, request);
		return ResponseEntity.ok(response);
	}

	@PutMapping(path = "changepassword", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseUserUpdatePassword> changePassword(
			@RequestHeader("Authorization") String token,
			@RequestBody RequestChangePassword request) {
		String userId = extractUserIdFromToken(token);
		ResponseUserUpdatePassword response = businessUser.changePassword(userId, request);
		return ResponseEntity.ok(response);
	}

	@PatchMapping(path = "deactivateuser/{idUser}", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseUserUpdate> deactivateUser(
			@PathVariable String idUser,
			@RequestBody RequestDeactivateUser request) {
		ResponseUserUpdate response = businessUser.deactivateUser(idUser, request);
		return ResponseEntity.ok(response);
	}

	@GetMapping(path = "indexuser")
	public ResponseEntity<Map<String, Object>> getAll() {
		return ResponseEntity.ok(businessUser.getAll());
	}

	@GetMapping(path = "showuser/{idUser}")
	public ResponseEntity<Map<String, Object>> getById(@PathVariable String idUser) {
		return ResponseEntity.ok(businessUser.getById(idUser));
	}

	@DeleteMapping(path = "deleteuser/{idUser}")
	public ResponseEntity<ResponseUserDeleteById> deleteById(@PathVariable String idUser) {
		ResponseUserDeleteById response = businessUser.deleteById(idUser);
		return ResponseEntity.ok(response);
	}

	@PutMapping(path = "updateuser/{idUser}", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseUserUpdate> updateUser(@PathVariable String idUser,
			@RequestBody RequestUserUpdate request) {
		ResponseUserUpdate response = businessUser.updateUser(idUser, request);
		return ResponseEntity.ok(response);
	}

	@PutMapping(path = "updatepassworduser/{email}")
	public ResponseEntity<ResponseUserUpdatePassword> updatePasswordUser(@PathVariable String email,
			@RequestBody RequestUserUpdatePassword request) {
		ResponseUserUpdatePassword response = businessUser.updatePasswordUser(email, request);
		return ResponseEntity.ok(response);
	}

	private String extractUserIdFromToken(String authHeader) {
		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			String token = authHeader.substring(7);
			return jwtHelper.extractUserId(token);
		}
		return null;
	}

	@PostMapping(path = "resetpassword/{idUser}")
	public ResponseEntity<ResponseUserInsert> resetPassword(@PathVariable String idUser) {
		ResponseUserInsert response = businessUser.resetPassword(idUser);
		return ResponseEntity.ok(response);
	}
}