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

import com.epiis.projectcasaketteler.dto.request.RequestDocumentGeneralInsert;
import com.epiis.projectcasaketteler.dto.response.ResponseDocumentGeneralInsert;
import com.epiis.projectcasaketteler.entity.EntityDocumentGeneral;
import com.epiis.projectcasaketteler.entity.EntityUser;
import com.epiis.projectcasaketteler.repository.RepositoryDocumentGeneral;
import com.epiis.projectcasaketteler.repository.RepositoryUser;

@Service
public class BusinessDocumentGeneral {
	@Autowired
	RepositoryDocumentGeneral repositoryDocumentGeneral;
	
	@Autowired
	RepositoryUser repositoryUser;
	
	private String storageDir = "storage";
	
	public ResponseDocumentGeneralInsert insert(RequestDocumentGeneralInsert request) throws Exception {
		ResponseDocumentGeneralInsert response = new ResponseDocumentGeneralInsert();
		
		Optional<EntityUser> optional = repositoryUser.findById(request.getIdUser());
		
		EntityUser entityUser = optional.get();
		
		Path storagePath = Paths.get(storageDir + "/DocumentGenral/" + entityUser.getFirstName() + request.getType());
		
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
			
			EntityDocumentGeneral entityDocumentGeneral = new EntityDocumentGeneral();
			
			entityDocumentGeneral.setIdDocumentGeneral(UUID.randomUUID().toString());
			entityDocumentGeneral.setParentUser(entityUser);
			entityDocumentGeneral.setType(request.getType());
			entityDocumentGeneral.setNameDocumentGeneral(fileName);
			entityDocumentGeneral.setExtensionDocumentGeneral(extension);
			entityDocumentGeneral.setCreated_at(new java.sql.Date(new Date().getTime()));
			entityDocumentGeneral.setUpdated_at(entityDocumentGeneral.getCreated_at());
			
			repositoryDocumentGeneral.save(entityDocumentGeneral);
		}
		
		response.success();
		response.getListMessage().add("Documentos Generales Registrados Correctamente");
		
		return response;
	}
}
