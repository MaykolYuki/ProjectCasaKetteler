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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.epiis.projectcasaketteler.dto.request.RequestDocumentEnterInsert;
import com.epiis.projectcasaketteler.dto.response.ResponseDocumentEnterInsert;
import com.epiis.projectcasaketteler.entity.EntityDocumentEnter;
import com.epiis.projectcasaketteler.entity.EntityUser;
import com.epiis.projectcasaketteler.exception.DocumentAccessException;
import com.epiis.projectcasaketteler.helper.DocumentValidationHelper;
import com.epiis.projectcasaketteler.repository.RepositoryDocumentEnter;
import com.epiis.projectcasaketteler.repository.RepositoryUser;

@Service
public class BusinessDocumentEnter {

	@Autowired
	private RepositoryDocumentEnter repositoryDocumentEnter;

	@Autowired
	private RepositoryUser repositoryUser;

	@Autowired
	private DocumentValidationHelper documentValidationHelper;

	@Autowired
	private com.epiis.projectcasaketteler.helper.DocumentNameHelper documentNameHelper;

	@Value("${app.storage.path}")
	private String storageDir;;

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

		Path storagePath = Paths.get(storageDir + "/DocumentEnter/" + entityUser.getIdUser());
		if (!Files.exists(storagePath)) {
			Files.createDirectories(storagePath);
		}

		String originalFileName = file.getOriginalFilename();
		String extension = "";
		if (originalFileName != null && originalFileName.contains(".")) {
			extension = originalFileName.substring(originalFileName.lastIndexOf(".") + 1);
		}

		String fileNameUUID = UUID.randomUUID().toString();
		// Nombre legible para el usuario: ENTRADA_Nombre_Apellido_fecha.ext
		String filePhysicalName = documentNameHelper.construirUnico(storagePath, "ENTRADA",
				entityUser.getFirstName(), entityUser.getSurName(), extension);

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

	public Map<String, Object> getByUser(String idUser) {
		Map<String, Object> res = new HashMap<>();
		Optional<EntityUser> optional = repositoryUser.findById(idUser);
		if (!optional.isPresent()) {
			res.put("type", "error");
			res.put("message", "Usuario no encontrado");
			res.put("data", null);
			return res;
		}

		List<EntityDocumentEnter> docs = repositoryDocumentEnter.findByParentUserOrderByCreated_atDesc(optional.get());

		res.put("type", "success");
		res.put("message", "Documentos de entrada obtenidos correctamente");
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

		List<EntityDocumentEnter> docs = repositoryDocumentEnter
				.findByParentUserAndDownloadableTrueOrderByCreated_atDesc(optional.get());

		res.put("type", "success");
		res.put("message", "Documentos descargables obtenidos correctamente");
		res.put("data", docs);
		return res;
	}

	public ResponseDocumentEnterInsert updateStatus(String idDocument, String status, String observations) {
		ResponseDocumentEnterInsert response = new ResponseDocumentEnterInsert();

		Optional<EntityDocumentEnter> optional = repositoryDocumentEnter.findById(idDocument);
		if (!optional.isPresent()) {
			response.error();
			response.getListMessage().add("Error: Documento no encontrado");
			return response;
		}

		EntityDocumentEnter doc = optional.get();
		doc.setStatus(EntityDocumentEnter.DocumentEnterStatus.valueOf(status));
		doc.setObservations(observations);

		if ("APROBADO".equals(status)) {
			doc.setDownloadable(true);
		}
		doc.setUpdated_at(new java.sql.Date(new Date().getTime()));

		repositoryDocumentEnter.save(doc);

		response.success();
		response.getListMessage().add("Documento actualizado correctamente");
		return response;
	}

	public org.springframework.core.io.Resource download(String idDocument, String requesterId, String requesterRole)
			throws Exception {
		Optional<EntityDocumentEnter> optional = repositoryDocumentEnter.findById(idDocument);
		if (!optional.isPresent()) {
			throw new DocumentAccessException("Documento no encontrado", HttpStatus.NOT_FOUND);
		}

		EntityDocumentEnter doc = optional.get();
		String idUser = doc.getParentUser().getIdUser();

		boolean esAdmin = "ADMIN".equals(requesterRole) || "SUPER_ADMIN".equals(requesterRole);
		boolean esDueno = idUser.equals(requesterId);

		if (!esAdmin && !esDueno) {
			throw new DocumentAccessException("Acceso denegado: este documento no te pertenece", HttpStatus.FORBIDDEN);
		}

		String filePath = storageDir + "/DocumentEnter/" + idUser + "/" + doc.getNameDocumentEnter();
		java.nio.file.Path path = java.nio.file.Paths.get(filePath);
		return new org.springframework.core.io.UrlResource(path.toUri());
	}
}