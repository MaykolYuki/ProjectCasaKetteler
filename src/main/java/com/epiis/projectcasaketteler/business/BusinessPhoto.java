package com.epiis.projectcasaketteler.business;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.epiis.projectcasaketteler.dto.request.RequestPhotoInsert;
import com.epiis.projectcasaketteler.dto.response.ResponsePhotoFilter;
import com.epiis.projectcasaketteler.dto.response.ResponsePhotoInsert;
import com.epiis.projectcasaketteler.entity.EntityPhoto;
import com.epiis.projectcasaketteler.entity.EntityUser;
import com.epiis.projectcasaketteler.helper.PythonFaceRecognitionHelper;
import com.epiis.projectcasaketteler.repository.RepositoryPhoto;
import com.epiis.projectcasaketteler.repository.RepositoryUser;

@Service
public class BusinessPhoto {
	@Autowired
	RepositoryPhoto repositoryPhoto;

	@Autowired
	RepositoryUser repositoryUser;

	@Autowired
	PythonFaceRecognitionHelper pythonFaceRecognitionHelper;

	private String storageDir = "storage";

	public ResponsePhotoInsert insert(RequestPhotoInsert request) throws Exception {
		ResponsePhotoInsert response = new ResponsePhotoInsert();

		Optional<EntityUser> optional = repositoryUser.findById(request.getIdUser());
		if (!optional.isPresent()) {
			response.error();
			response.getListMessage().add("Error: Usuario no encontrado");
			return response;
		}

		EntityUser entityUser = optional.get();

		Path storagePath = Paths.get(storageDir + "/Photo/" + entityUser.getIdUser());
		if (!Files.exists(storagePath)) {
			Files.createDirectories(storagePath);
		}

		MultipartFile[] files = request.getFiles();
		List<String> savedFileNames = new ArrayList<>();

		// UN SOLO LOOP — guarda y registra nombre
		if (files != null && files.length > 0) {
			for (MultipartFile file : files) {
				if (file.isEmpty())
					continue;

				String originalFileName = file.getOriginalFilename();
				String extension = "";
				if (originalFileName != null && originalFileName.contains(".")) {
					extension = originalFileName.substring(originalFileName.lastIndexOf(".") + 1);
				}

				String fileName = UUID.randomUUID().toString() + "." + extension;
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

				savedFileNames.add(fileName); // registrar nombre para el reporte
			}
		}

		// Reporte de calidad — UNA SOLA llamada al filtro
		List<Map<String, Object>> qualityReport = new ArrayList<>();

		ResponsePhotoFilter filtro = pythonFaceRecognitionHelper
				.seleccionarMejorFoto(storageDir + "/Photo/" + entityUser.getIdUser());

		// GUARDAR la mejor foto en el usuario para reutilizarla después
		if (filtro.isSuccess() && filtro.getBestImage() != null) {
			entityUser.setBestPhotoReference(filtro.getBestImage());
			repositoryUser.save(entityUser);
		}
		// Después de llamar al filtro, antes del loop del reporte
		System.out.println("=== DEBUG FILTRO ===");
		System.out.println("Best image del filtro: " + filtro.getBestImage());
		System.out.println("Archivos guardados ahora: " + savedFileNames);
		for (String savedFileName : savedFileNames) {
			Map<String, Object> photoInfo = new HashMap<>();
			photoInfo.put("archivo", savedFileName);

			if (filtro.isSuccess() && filtro.getBestImage() != null
					&& filtro.getBestImage().equals(savedFileName)) {
				photoInfo.put("calidad", "OPTIMA");
				photoInfo.put("nitidez", filtro.getSharpnessScore());
				photoInfo.put("mensaje", "Esta foto fue seleccionada como la mejor referencia");
			} else if (filtro.isSuccess()) {
				photoInfo.put("calidad", "ACEPTABLE");
				photoInfo.put("mensaje", "Foto válida pero no es la de mayor calidad");
			} else {
				photoInfo.put("calidad", "RECHAZADA");
				photoInfo.put("mensaje", filtro.getError());
			}

			qualityReport.add(photoInfo);
		}

		response.success();
		response.getListMessage().add("Fotos registradas correctamente");
		response.setPhotoQualityReport(qualityReport);

		return response;
	}
}
