package com.epiis.projectcasaketteler.helper;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("Nombres de documentos: patrón TIPO_Nombre_Apellido_fecha")
class DocumentNameHelperTest {

	@TempDir
	Path carpeta;

	private DocumentNameHelper helper;

	@BeforeEach
	void setUp() {
		helper = new DocumentNameHelper();
	}

	@Test
	@DisplayName("Construye el nombre con el tipo, el nombre completo y la fecha de hoy")
	void construyeNombreLegible() {
		String nombre = helper.construirUnico(carpeta, "DNI", "Juan", "Perez", "pdf");

		assertThat(nombre).isEqualTo("DNI_Juan_Perez_" + LocalDate.now() + ".pdf");
	}

	@Test
	@DisplayName("Quita tildes y espacios para que el nombre sea válido en disco")
	void limpiaTildesYEspacios() {
		String nombre = helper.construirUnico(carpeta, "FICHA", "José Ángel", "Núñez Paz", "docx");

		assertThat(nombre).isEqualTo("FICHA_JoseAngel_NunezPaz_" + LocalDate.now() + ".docx");
	}

	@Test
	@DisplayName("Si ya existe un archivo igual, agrega un sufijo en vez de sobrescribirlo")
	void evitaSobrescribirDocumentoExistente() throws Exception {
		String primero = helper.construirUnico(carpeta, "PAGO", "Ana", "Lopez", "pdf");
		Files.createFile(carpeta.resolve(primero));

		String segundo = helper.construirUnico(carpeta, "PAGO", "Ana", "Lopez", "pdf");

		assertThat(segundo).isNotEqualTo(primero);
		assertThat(segundo).isEqualTo("PAGO_Ana_Lopez_" + LocalDate.now() + "_2.pdf");
	}

	@Test
	@DisplayName("Sin extensión no deja un punto suelto al final")
	void manejaArchivoSinExtension() {
		String nombre = helper.construirUnico(carpeta, "OTRO", "Ana", "Lopez", "");

		assertThat(nombre).doesNotEndWith(".");
		assertThat(nombre).isEqualTo("OTRO_Ana_Lopez_" + LocalDate.now());
	}

	@Test
	@DisplayName("Si falta el nombre del residente no rompe: usa un marcador")
	void toleraDatosFaltantes() {
		String nombre = helper.construirUnico(carpeta, "DNI", null, "", "pdf");

		assertThat(nombre).isEqualTo("DNI_SD_SD_" + LocalDate.now() + ".pdf");
	}
}
