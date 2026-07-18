package com.epiis.projectcasaketteler.business;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.epiis.projectcasaketteler.dto.request.RequestDocumentResignationInsert;
import com.epiis.projectcasaketteler.dto.response.ResponseDocumentResignationInsert;
import com.epiis.projectcasaketteler.entity.EntityDocumentResignation;
import com.epiis.projectcasaketteler.entity.EntityUser;
import com.epiis.projectcasaketteler.exception.DocumentAccessException;
import com.epiis.projectcasaketteler.helper.DocumentValidationHelper;
import com.epiis.projectcasaketteler.repository.RepositoryDocumentResignation;
import com.epiis.projectcasaketteler.repository.RepositoryUser;

@Service
public class BusinessDocumentResignation {
	@Autowired
	private RepositoryDocumentResignation repositoryDocumentResignation;

	@Autowired
	private RepositoryUser repositoryUser;

	@Autowired
	private DocumentValidationHelper documentValidationHelper;

	@Value("${app.storage.path}")
	private String storageDir;

	public ResponseDocumentResignationInsert insert(RequestDocumentResignationInsert request) throws Exception {
		ResponseDocumentResignationInsert response = new ResponseDocumentResignationInsert();

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

		Path storagePath = Paths.get(storageDir + "/DocumentResignation/" + entityUser.getIdUser());

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

		// Reutilizar el registro existente (p. ej. si el admin ya asignó el formato) en
		// vez de duplicar
		Optional<EntityDocumentResignation> existing = repositoryDocumentResignation
				.findTopByParentUserOrderByCreated_atDesc(entityUser);

		EntityDocumentResignation entityDocumentResignation;
		if (existing.isPresent()) {
			entityDocumentResignation = existing.get();
		} else {
			entityDocumentResignation = new EntityDocumentResignation();
			entityDocumentResignation.setIdDocumentResignation(fileNameUUID);
			entityDocumentResignation.setParentUser(entityUser);
			entityDocumentResignation.setCreated_at(new java.sql.Date(new Date().getTime()));
		}

		entityDocumentResignation.setNameDocumentResignation(filePhysicalName);
		entityDocumentResignation.setExtensionDocumentResignation(extension);
		entityDocumentResignation.setStatus(EntityDocumentResignation.ResignationStatus.PENDIENTE);
		entityDocumentResignation.setUpdated_at(new java.sql.Date(new Date().getTime()));

		repositoryDocumentResignation.save(entityDocumentResignation);

		response.success();
		response.getListMessage().add("Documento de Renuncia Registrado Exitosamente");

		return response;
	}

	public Map<String, Object> getByUser(String idUser) {
		Map<String, Object> res = new HashMap<>();
		Optional<EntityUser> optional = repositoryUser.findById(idUser);
		if (!optional.isPresent()) {
			res.put("type", "error");
			res.put("message", "Usuario no encontrado");
			res.put("data", null);
			return res;
		}

		Optional<EntityDocumentResignation> doc = repositoryDocumentResignation
				.findTopByParentUserOrderByCreated_atDesc(optional.get());

		res.put("type", "success");
		res.put("message", "Renuncia obtenida correctamente");
		res.put("data", doc.orElse(null));
		return res;
	}

	public Map<String, Object> getMyResignation(String idUser) {
		Map<String, Object> res = new HashMap<>();

		Optional<EntityUser> optional = repositoryUser.findById(idUser);
		if (!optional.isPresent()) {
			res.put("type", "error");
			res.put("message", "Usuario no encontrado");
			res.put("data", null);
			return res;
		}

		Optional<EntityDocumentResignation> doc = repositoryDocumentResignation
				.findTopByParentUserOrderByCreated_atDesc(optional.get());

		res.put("type", "success");
		res.put("message", "Renuncia obtenida correctamente");
		res.put("data", doc.orElse(null));
		return res;
	}

	public ResponseDocumentResignationInsert updateStatus(
			String idDocument, String status, String observations) {
		ResponseDocumentResignationInsert response = new ResponseDocumentResignationInsert();

		Optional<EntityDocumentResignation> optional = repositoryDocumentResignation.findById(idDocument);

		if (!optional.isPresent()) {
			response.error();
			response.getListMessage().add("Error: Documento no encontrado");
			return response;
		}

		EntityDocumentResignation doc = optional.get();
		doc.setStatus(EntityDocumentResignation.ResignationStatus.valueOf(status));
		doc.setObservations(observations);
		doc.setUpdated_at(new java.sql.Date(new Date().getTime()));

		repositoryDocumentResignation.save(doc);

		response.success();
		response.getListMessage().add("Estado actualizado correctamente");
		return response;
	}

	public org.springframework.core.io.Resource download(String idDocument, String requesterId, String requesterRole)
			throws Exception {
		Optional<EntityDocumentResignation> optional = repositoryDocumentResignation.findById(idDocument);
		if (!optional.isPresent()) {
			throw new DocumentAccessException("Documento no encontrado", HttpStatus.NOT_FOUND);
		}

		EntityDocumentResignation doc = optional.get();
		String idUser = doc.getParentUser().getIdUser();

		boolean esAdmin = "ADMIN".equals(requesterRole) || "SUPER_ADMIN".equals(requesterRole);
		boolean esDueno = idUser.equals(requesterId);

		if (!esAdmin && !esDueno) {
			throw new DocumentAccessException("Acceso denegado: este documento no te pertenece", HttpStatus.FORBIDDEN);
		}

		String filePath = storageDir + "/DocumentResignation/" + idUser + "/" + doc.getNameDocumentResignation();
		java.nio.file.Path path = java.nio.file.Paths.get(filePath);
		return new org.springframework.core.io.UrlResource(path.toUri());
	}

	// RF-25: Admin sube formato en blanco de renuncia y lo asigna al residente
	public ResponseDocumentResignationInsert assignFormat(String idUser, MultipartFile file) throws Exception {
		ResponseDocumentResignationInsert response = new ResponseDocumentResignationInsert();

		Optional<EntityUser> optional = repositoryUser.findById(idUser);
		if (!optional.isPresent()) {
			response.error();
			response.getListMessage().add("Error: Usuario no encontrado");
			return response;
		}
		EntityUser entityUser = optional.get();

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

		Path storagePath = Paths.get(storageDir + "/ResignationFormat/" + entityUser.getIdUser());
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

		// Buscar si ya existe un registro de renuncia para este usuario, o crear uno
		// nuevo
		Optional<EntityDocumentResignation> existing = repositoryDocumentResignation
				.findTopByParentUserOrderByCreated_atDesc(entityUser);

		EntityDocumentResignation entity;
		if (existing.isPresent()) {
			entity = existing.get();
		} else {
			entity = new EntityDocumentResignation();
			entity.setIdDocumentResignation(UUID.randomUUID().toString());
			entity.setParentUser(entityUser);
			entity.setStatus(EntityDocumentResignation.ResignationStatus.PENDIENTE);
			entity.setCreated_at(new java.sql.Date(new Date().getTime()));
		}

		entity.setFormatFileName(filePhysicalName);
		entity.setFormatExtension(extension);
		entity.setFormatAssignedAt(new Date());
		entity.setUpdated_at(new java.sql.Date(new Date().getTime()));

		repositoryDocumentResignation.save(entity);

		response.success();
		response.getListMessage().add("Formato de renuncia asignado correctamente");
		return response;
	}

	// RF-26: Residente descarga el formato asignado por admin
	public org.springframework.core.io.Resource downloadFormat(String idUser) throws Exception {
		Optional<EntityUser> optional = repositoryUser.findById(idUser);
		if (!optional.isPresent()) {
			throw new RuntimeException("Usuario no encontrado");
		}
		EntityUser entityUser = optional.get();

		Optional<EntityDocumentResignation> doc = repositoryDocumentResignation
				.findTopByParentUserOrderByCreated_atDesc(entityUser);

		if (!doc.isPresent() || doc.get().getFormatFileName() == null) {
			throw new RuntimeException("No hay formato de renuncia asignado");
		}

		String filePath = storageDir + "/ResignationFormat/" +
				entityUser.getIdUser() + "/" +
				doc.get().getFormatFileName();

		java.nio.file.Path path = java.nio.file.Paths.get(filePath);
		return new org.springframework.core.io.UrlResource(path.toUri());
	}
}
