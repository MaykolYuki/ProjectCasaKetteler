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
import com.epiis.projectcasaketteler.helper.DocumentValidationHelper;
import com.epiis.projectcasaketteler.repository.RepositoryDocumentEnter;
import com.epiis.projectcasaketteler.repository.RepositoryUser;

@Service
public class BusinessDocumentEnter {

	@Autowired
	RepositoryDocumentEnter repositoryDocumentEnter;

	@Autowired
	RepositoryUser repositoryUser;

	@Autowired
	DocumentValidationHelper documentValidationHelper;

	private String storageDir = "storage";

	public ResponseDocumentEnterInsert insert(RequestDocumentEnterInsert request) throws Exception {
		ResponseDocumentEnterInsert response = new ResponseDocumentEnterInsert();

		Optional<EntityUser> optional = repositoryUser.findById(request.getIdUser());
		if (!optional.isPresent()) {
			response.error();
			response.getListMessage().add("Error: Usuario no encontrado");
			return response;
		}
		EntityUser entityUser = optional.get();

		MultipartFile file = request.getFile();
		if (file == null || file.isEmpty()) {
			response.error();
			response.getListMessage().add("Error: No se ha adjuntado ningún archivo o está vacío");
			return response;
		}

		String validationError = documentValidationHelper.validate(file);
		if (validationError != null) {
			response.error();
			response.getListMessage().add("Error: " + validationError);
			return response;
		}

		Path storagePath = Paths.get(storageDir + "/DocumentEnter/" + entityUser.getFirstName());
		if (!Files.exists(storagePath)) {
			Files.createDirectories(storagePath);
		}

		String originalFileName = file.getOriginalFilename();
		String extension = "";
		if (originalFileName != null && originalFileName.contains(".")) {
			extension = originalFileName.substring(originalFileName.lastIndexOf(".") + 1);
		}

		String fileNameUUID = UUID.randomUUID().toString();
		String filePhysicalName = extension.isEmpty() ? fileNameUUID : fileNameUUID + "." + extension;

		Path filePath = storagePath.resolve(filePhysicalName);
		Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

		EntityDocumentEnter entityDocumentEnter = new EntityDocumentEnter();
		entityDocumentEnter.setIdDocumentEnter(fileNameUUID); 
		entityDocumentEnter.setParentUser(entityUser);
		entityDocumentEnter.setNameDocumentEnter(filePhysicalName); 
		entityDocumentEnter.setExtensionDocumentEnter(extension);
		entityDocumentEnter.setCreated_at(new java.sql.Date(new Date().getTime()));

		repositoryDocumentEnter.save(entityDocumentEnter);

		response.success();
		response.getListMessage().add("Documento de Entrada Registrado Exitosamente");

		return response;
	}
}