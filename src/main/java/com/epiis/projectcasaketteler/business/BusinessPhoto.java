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

import com.epiis.projectcasaketteler.dto.request.RequestPhotoInsert;
import com.epiis.projectcasaketteler.dto.response.ResponsePhotoInsert;
import com.epiis.projectcasaketteler.entity.EntityPhoto;
import com.epiis.projectcasaketteler.entity.EntityUser;
import com.epiis.projectcasaketteler.repository.RepositoryPhoto;
import com.epiis.projectcasaketteler.repository.RepositoryUser;

@Service
public class BusinessPhoto {
	@Autowired
	RepositoryPhoto repositoryPhoto;
	
	@Autowired
	RepositoryUser repositoryUser;
	
	private String storageDir =  "storage";
	
	public ResponsePhotoInsert insert(RequestPhotoInsert request) throws Exception {
		ResponsePhotoInsert response = new ResponsePhotoInsert();
		
		Optional<EntityUser> optional = repositoryUser.findById(request.getIdUser());
		
		EntityUser entityUser = optional.get();
		
		Path storagePath = Paths.get(storageDir + "/Photo/" + entityUser.getFirstName());
		
		if (!Files.exists(storagePath)) {
			Files.createDirectories(storagePath);
		}
		
		MultipartFile[] files = request.getFiles();
		
		if (files != null && files.length > 0) {
			for (MultipartFile file : files) {
				if (file.isEmpty()) {
					continue;
				}
				
				String originalFileName = file.getOriginalFilename();
				
				String extension = "";
				
				if (originalFileName != null && originalFileName.contains(".")) {
					extension = originalFileName.substring(originalFileName.lastIndexOf(".") + 1);
				}
				
				String fileName = UUID.randomUUID().toString();
				
				Path filePath = storagePath.resolve(fileName);
				
				Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
				
				EntityPhoto entityPhoto = new EntityPhoto();
				
				entityPhoto.setIdPhoto(UUID.randomUUID().toString());
				entityPhoto.setParentUser(entityUser);
				entityPhoto.setNamePhoto(fileName);
				entityPhoto.setExtensionPhoto(extension);
				entityPhoto.setCreated_at(new java.sql.Date(new Date().getTime()));
				entityPhoto.setUpdated_at(entityPhoto.getCreated_at());
				
				repositoryPhoto.save(entityPhoto);
			}
		}
		
		response.success();
		response.getListMessage().add("Foto del Estudiante Registrada Correctamente");
		
		return response;
	}
}
