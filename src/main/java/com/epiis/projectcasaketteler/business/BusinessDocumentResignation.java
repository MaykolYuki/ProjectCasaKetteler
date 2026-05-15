package com.epiis.projectcasaketteler.business;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.epiis.projectcasaketteler.dto.request.RequestDocumentResignationInsert;
import com.epiis.projectcasaketteler.dto.response.ResponseDocumentResignationInsert;
import com.epiis.projectcasaketteler.entity.EntityDocumentResignation;
import com.epiis.projectcasaketteler.entity.EntityUser;
import com.epiis.projectcasaketteler.repository.RepositoryDocumentResignation;
import com.epiis.projectcasaketteler.repository.RepositoryUser;

@Service
public class BusinessDocumentResignation {
	@Autowired
	RepositoryDocumentResignation repositoryDocumentResignation;
	
	@Autowired
	RepositoryUser repositoryUser;
	
	private String storageDir = "storage"; 
	
	public ResponseDocumentResignationInsert  insert(RequestDocumentResignationInsert request) throws Exception {
		ResponseDocumentResignationInsert response = new ResponseDocumentResignationInsert();
		
		Optional<EntityUser> optional = repositoryUser.findById(request.getIdUser());
		
		EntityUser entityUser = optional.get();
		
		Path storagePath = Paths.get(storageDir + "/DocumentResignation/" + entityUser.getFirstName());
		
		if (!Files.exists(storagePath)) {
			Files.createDirectories(storagePath);
		}
		
		MultipartFile file = request.getFile();
		
		if (file != null) {
			String originalFileName = file.getOriginalFilename();
			
			String extension = "";
			
			if (originalFileName != null && originalFileName.contains(".")) {
				extension = originalFileName.substring(originalFileName.lastIndexOf(".") + 1);
			}
			
			String fileName = UUID.randomUUID().toString();
			
			Path filePath = storagePath.resolve(fileName);
			
			Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
			
			EntityDocumentResignation entityDocumentResignation = new EntityDocumentResignation();
			
			entityDocumentResignation.setIdDocumentResignation(UUID.randomUUID().toString());
			entityDocumentResignation.setParentUser(entityUser);
			entityDocumentResignation.setNameDocumentResignation(fileName);
			entityDocumentResignation.setExtensionDocumentResignation(extension);
			entityDocumentResignation.setStatus(true);
			entityDocumentResignation.setCreated_at(new java.sql.Date(new Date().getTime()));
			entityDocumentResignation.setUpdated_at(entityDocumentResignation.getCreated_at());
			
			repositoryDocumentResignation.save(entityDocumentResignation);
		}
		
		response.success();
		response.getListMessage().add("Documento de Renuncia Registrado Exitosamente");
		
		return response;
	}
}
