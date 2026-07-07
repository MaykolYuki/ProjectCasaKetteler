package com.epiis.projectcasaketteler.business;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.epiis.projectcasaketteler.dto.request.RequestDocumentGeneralInsert;
import com.epiis.projectcasaketteler.dto.response.ResponseDocumentGeneralInsert;
import com.epiis.projectcasaketteler.entity.EntityDocumentGeneral;
import com.epiis.projectcasaketteler.entity.EntityUser;
import com.epiis.projectcasaketteler.helper.DocumentValidationHelper;
import com.epiis.projectcasaketteler.repository.RepositoryDocumentGeneral;
import com.epiis.projectcasaketteler.repository.RepositoryUser;

@Service
public class BusinessDocumentGeneral {

	@Autowired
	RepositoryDocumentGeneral repositoryDocumentGeneral;

	@Autowired
	RepositoryUser repositoryUser;

	@Autowired
	DocumentValidationHelper documentValidationHelper;

	private String storageDir = "storage";

	public ResponseDocumentGeneralInsert insert(RequestDocumentGeneralInsert request) throws Exception {
		ResponseDocumentGeneralInsert response = new ResponseDocumentGeneralInsert();

		Optional<EntityUser> optional = repositoryUser.findById(request.getIdUser());

		if (!optional.isPresent()) {
			response.error();
			response.getListMessage().add("Error: Usuario no encontrado");
			return response;
		}

		EntityUser entityUser = optional.get();

		if ("PAGO".equalsIgnoreCase(request.getType())) {
			String period = getCurrentMonthPeriod();
			Optional<EntityDocumentGeneral> existing = repositoryDocumentGeneral
					.findByParentUserAndTypeAndPeriod(entityUser, request.getType(), period);
			if (existing.isPresent()) {
				response.error();
				response.getListMessage().add(
						"Error: Ya subiste tu constancia de pago este mes (" + period + ")");
				return response;
			}
		}

		if ("NOTAS".equalsIgnoreCase(request.getType())) {
			String period = getCurrentSemesterPeriod();
			Optional<EntityDocumentGeneral> existing = repositoryDocumentGeneral
					.findByParentUserAndTypeAndPeriod(entityUser, request.getType(), period);
			if (existing.isPresent()) {
				response.error();
				response.getListMessage().add(
						"Error: Ya subiste tu constancia de notas este semestre (" + period + ")");
				return response;
			}
		}

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

		Path storagePath = Paths.get(storageDir + "/DocumentGeneral/" + entityUser.getFirstName() + "/" + request.getType());

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

		EntityDocumentGeneral entityDocumentGeneral = new EntityDocumentGeneral();
		entityDocumentGeneral.setIdDocumentGeneral(fileNameUUID);
		entityDocumentGeneral.setParentUser(entityUser);
		entityDocumentGeneral.setType(request.getType());
		entityDocumentGeneral.setPeriod(
				"PAGO".equalsIgnoreCase(request.getType()) ? getCurrentMonthPeriod()
						: "NOTAS".equalsIgnoreCase(request.getType()) ? getCurrentSemesterPeriod() : null);
		
		entityDocumentGeneral.setNameDocumentGeneral(filePhysicalName);
		entityDocumentGeneral.setExtensionDocumentGeneral(extension);
		entityDocumentGeneral.setCreated_at(new java.sql.Date(new Date().getTime()));
		entityDocumentGeneral.setUpdated_at(entityDocumentGeneral.getCreated_at());

		repositoryDocumentGeneral.save(entityDocumentGeneral);

		response.success();
		response.getListMessage().add("Documentos Generales Registrados Correctamente");

		return response;
	}

	public Map<String, Object> getByUser(String idUser, String type) {
		Map<String, Object> res = new HashMap<>();

		Optional<EntityUser> optional = repositoryUser.findById(idUser);
		if (!optional.isPresent()) {
			res.put("type", "error");
			res.put("message", "Usuario no encontrado");
			res.put("data", null);
			return res;
		}

		EntityUser user = optional.get();
		List<EntityDocumentGeneral> docs;

		if (type != null && !type.isEmpty()) {
			docs = repositoryDocumentGeneral
					.findByParentUserAndTypeOrderByCreated_atDesc(user, type);
		} else {
			docs = repositoryDocumentGeneral
					.findByParentUserOrderByCreated_atDesc(user);
		}

		res.put("type", "success");
		res.put("message", "Documentos obtenidos correctamente");
		res.put("data", docs);
		return res;
	}

	public Map<String, Object> getMyDownloadableDocuments(String idUser) {
		Map<String, Object> res = new HashMap<>();

		Optional<EntityUser> optional = repositoryUser.findById(idUser);
		if (!optional.isPresent()) {
			res.put("type", "error");
			res.put("message", "Usuario no encontrado");
			res.put("data", null);
			return res;
		}

		List<EntityDocumentGeneral> docs = repositoryDocumentGeneral
				.findByParentUserAndDownloadableTrueOrderByCreated_atDesc(optional.get());

		res.put("type", "success");
		res.put("message", "Documentos descargables obtenidos correctamente");
		res.put("data", docs);
		return res;
	}

	public ResponseDocumentGeneralInsert updateStatus(String idDocument, String status, String observations) {
		ResponseDocumentGeneralInsert response = new ResponseDocumentGeneralInsert();

		Optional<EntityDocumentGeneral> optional = repositoryDocumentGeneral.findById(idDocument);
		if (!optional.isPresent()) {
			response.error();
			response.getListMessage().add("Error: Documento no encontrado");
			return response;
		}

		EntityDocumentGeneral doc = optional.get();
		doc.setStatus(EntityDocumentGeneral.DocumentStatus.valueOf(status));
		doc.setObservations(observations);
		doc.setUpdated_at(new java.sql.Date(new Date().getTime()));

		repositoryDocumentGeneral.save(doc);

		response.success();
		response.getListMessage().add("Documento actualizado correctamente");
		return response;
	}

	public org.springframework.core.io.Resource download(String idDocument) throws Exception {
		Optional<EntityDocumentGeneral> optional = repositoryDocumentGeneral.findById(idDocument);
		if (!optional.isPresent()) {
			throw new RuntimeException("Documento no encontrado");
		}

		EntityDocumentGeneral doc = optional.get();
		String filePath = storageDir + "/DocumentGeneral/" +
				doc.getParentUser().getFirstName() + "/" +
				doc.getType() + "/" +
				doc.getNameDocumentGeneral();

		java.nio.file.Path path = java.nio.file.Paths.get(filePath);
		return new org.springframework.core.io.UrlResource(path.toUri());
	}

	private String getCurrentMonthPeriod() {
		java.time.LocalDate now = java.time.LocalDate.now();
		return now.getYear() + "-" + String.format("%02d", now.getMonthValue());
	}

	private String getCurrentSemesterPeriod() {
		java.time.LocalDate now = java.time.LocalDate.now();
		int semester = now.getMonthValue() <= 6 ? 1 : 2;
		return now.getYear() + "-" + semester;
	}
}