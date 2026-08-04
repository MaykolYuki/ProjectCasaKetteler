package com.epiis.projectcasaketteler.controller;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.epiis.projectcasaketteler.service.BackupService;

/**
 * Permite al administrador generar un respaldo en el momento (por ejemplo, antes de
 * una actualización), sin esperar al respaldo automático de la madrugada.
 */
@RestController
@RequestMapping(path = "casaketteler")
public class BackupController {

	@Autowired
	private BackupService backupService;

	@PostMapping(path = "backup")
	public ResponseEntity<Map<String, Object>> respaldarAhora() {
		Map<String, Object> respuesta = new HashMap<>();
		List<String> mensajes = new ArrayList<>();

		File baseDatos = backupService.crearRespaldo();
		File archivos = backupService.crearRespaldoArchivos();

		if (baseDatos == null && archivos == null) {
			respuesta.put("type", "error");
			mensajes.add("No se pudo generar el respaldo. Revisa los registros del sistema.");
			respuesta.put("listMessage", mensajes);
			return ResponseEntity.ok(respuesta);
		}

		if (baseDatos != null) {
			mensajes.add("Base de datos respaldada: " + baseDatos.getName());
		} else {
			mensajes.add("Advertencia: no se pudo respaldar la base de datos.");
		}

		if (archivos != null) {
			mensajes.add("Fotos y documentos respaldados: " + archivos.getName());
		} else {
			mensajes.add("Advertencia: no se pudieron respaldar las fotos y documentos.");
		}

		respuesta.put("type", "success");
		respuesta.put("listMessage", mensajes);
		return ResponseEntity.ok(respuesta);
	}
}
