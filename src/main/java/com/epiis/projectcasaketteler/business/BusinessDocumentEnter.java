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

import com.epiis.projectcasaketteler.dto.request.RequestDocumentEnterInsert;
import com.epiis.projectcasaketteler.dto.response.ResponseDocumentEnterInsert;
import com.epiis.projectcasaketteler.entity.EntityDocumentEnter;
import com.epiis.projectcasaketteler.entity.EntityUser;
import com.epiis.projectcasaketteler.repository.RepositoryDocumentEnter;
import com.epiis.projectcasaketteler.repository.RepositoryUser;

@Service
public class BusinessDocumentEnter {
	@Autowired
	RepositoryDocumentEnter repositoryDocumentEnter;
	
	@Autowired
	RepositoryUser repositoryUser;
	
	private String storageDir = "storage";
	
	public ResponseDocumentEnterInsert insert(RequestDocumentEnterInsert request) throws Exception {
		ResponseDocumentEnterInsert response = new ResponseDocumentEnterInsert();
		
		Optional<EntityUser> optional = repositoryUser.findById(request.getIdUser());
		
		EntityUser entityUser = optional.get();
		
		Path storagePath = Paths.get(storageDir + "/DocumentEnter/" + entityUser.getFirstName());
		
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
			
			EntityDocumentEnter entityDocumentEnter = new EntityDocumentEnter();
			
			entityDocumentEnter.setIdDocumentEnter(UUID.randomUUID().toString());
			entityDocumentEnter.setParentUser(entityUser);
			entityDocumentEnter.setNameDocumentEnter(fileName);
			entityDocumentEnter.setExtensionDocumentEnter(extension);
			entityDocumentEnter.setCreated_at(new java.sql.Date(new Date().getTime()));
			
			repositoryDocumentEnter.save(entityDocumentEnter);
		}
		
		response.success();
		response.getListMessage().add("Documentos de Entrada Registrados Exitosamente");
		
		return response;
	}
}
