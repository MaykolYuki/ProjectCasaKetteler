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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.epiis.projectcasaketteler.business.BusinessUser;
import com.epiis.projectcasaketteler.dto.request.RequestUserInsert;
import com.epiis.projectcasaketteler.dto.request.RequestUserUpdate;
import com.epiis.projectcasaketteler.dto.request.RequestUserUpdatePassword;
import com.epiis.projectcasaketteler.dto.response.ResponseUserDeleteById;
import com.epiis.projectcasaketteler.dto.response.ResponseUserInsert;
import com.epiis.projectcasaketteler.dto.response.ResponseUserUpdate;
import com.epiis.projectcasaketteler.dto.response.ResponseUserUpdatePassword;

@RestController
@RequestMapping(path = "casaketteler")
public class UserController {
	@Autowired
	BusinessUser businessUser;
	
	@PostMapping(path = "registeruser", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseUserInsert> insert(@RequestBody RequestUserInsert request){
		ResponseUserInsert response = businessUser.insert(request);
		
		return ResponseEntity.ok(response);
	}
	
	@GetMapping(path = "indexuser")
	public ResponseEntity<Map<String, Object>> getAll(){
		
		return ResponseEntity.ok(businessUser.getAll());
	}
	
	@GetMapping(path = "showuser/{idUser}")
	public ResponseEntity<Map<String, Object>> getById(@PathVariable String idUser){
		
		return ResponseEntity.ok(businessUser.getById(idUser));
	}
	
	@DeleteMapping(path = "deleteuser/{idUser}")
	public ResponseEntity<ResponseUserDeleteById> deleteById(@PathVariable String idUser){
		ResponseUserDeleteById response = businessUser.deleteById(idUser);
		
		return ResponseEntity.ok(response);
	}
	
	@PutMapping(path = "updateuser/{idUser}", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseUserUpdate> update(@PathVariable String idUser, @RequestBody RequestUserUpdate request){
		ResponseUserUpdate response = businessUser.update(idUser, request);
		
		return ResponseEntity.ok(response);
	}
	
	@PutMapping(path = "updatepassworduser/{email}")
	public ResponseEntity<ResponseUserUpdatePassword> updatePassword(@PathVariable String email, @RequestBody RequestUserUpdatePassword request){
		ResponseUserUpdatePassword response = businessUser.updatePassword(email, request);
		
		return ResponseEntity.ok(response);
	}
}
