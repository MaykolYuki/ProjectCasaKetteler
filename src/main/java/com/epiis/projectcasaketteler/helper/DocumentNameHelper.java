package com.epiis.projectcasaketteler.helper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.LocalDate;

import org.springframework.stereotype.Component;

/**
 * Construye nombres de archivo legibles con el patrón TIPO_Nombre_Apellido_fecha.
 * Antes se guardaba solo un UUID, que resultaba ininteligible para el usuario.
 */
@Component
public class DocumentNameHelper {

	/**
	 * Devuelve un nombre único dentro de la carpeta indicada. Si ya existe uno igual
	 * (mismo tipo, usuario y día), agrega un sufijo _2, _3, ... en vez de sobrescribir.
	 */
	public String construirUnico(Path carpeta, String tipo, String nombre, String apellido, String extension) {
		String base = limpiar(tipo) + "_" + limpiar(nombre) + "_" + limpiar(apellido) + "_" + LocalDate.now();
		String sufijoExtension = (extension == null || extension.isBlank()) ? "" : "." + extension;

		String candidato = base + sufijoExtension;
		int n = 2;
		while (Files.exists(carpeta.resolve(candidato))) {
			candidato = base + "_" + n + sufijoExtension;
			n++;
		}
		return candidato;
	}

	/** Quita tildes y caracteres no alfanuméricos para que el nombre sea seguro en disco. */
	private String limpiar(String texto) {
		if (texto == null || texto.isBlank()) {
			return "SD";
		}
		String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD)
				.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
		String limpio = sinTildes.trim().replaceAll("[^A-Za-z0-9]+", "");
		return limpio.isEmpty() ? "SD" : limpio;
	}
}
