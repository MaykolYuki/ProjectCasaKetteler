package com.epiis.projectcasaketteler.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Respaldo de fotos y documentos. La base de datos por sí sola no basta:
 * sin estos archivos el reconocimiento facial deja de funcionar.
 */
@DisplayName("Respaldo de fotos y documentos")
class BackupServiceTest {

	@TempDir
	Path carpetaArchivos;

	@TempDir
	Path carpetaRespaldos;

	private BackupService backupService;

	@BeforeEach
	void setUp() {
		backupService = new BackupService();
		ReflectionTestUtils.setField(backupService, "carpetaArchivos", carpetaArchivos.toString());
		ReflectionTestUtils.setField(backupService, "carpetaRespaldos", carpetaRespaldos.toString());
		ReflectionTestUtils.setField(backupService, "diasQueSeConservan", 14);
	}

	@Test
	@DisplayName("Comprime las fotos y documentos conservando su estructura de carpetas")
	void comprimeArchivosConSuEstructura() throws Exception {
		Files.createDirectories(carpetaArchivos.resolve("Photo/residente-1"));
		Files.writeString(carpetaArchivos.resolve("Photo/residente-1/rostro.jpg"), "foto");
		Files.createDirectories(carpetaArchivos.resolve("DocumentGeneral/residente-1"));
		Files.writeString(carpetaArchivos.resolve("DocumentGeneral/residente-1/DNI.pdf"), "documento");

		File comprimido = backupService.crearRespaldoArchivos();

		assertThat(comprimido).isNotNull().exists();
		try (ZipFile zip = new ZipFile(comprimido)) {
			assertThat(zip.getEntry("Photo/residente-1/rostro.jpg")).isNotNull();
			assertThat(zip.getEntry("DocumentGeneral/residente-1/DNI.pdf")).isNotNull();
		}
	}

	@Test
	@DisplayName("Si no existe la carpeta de archivos no rompe el sistema")
	void toleraCarpetaInexistente() {
		ReflectionTestUtils.setField(backupService, "carpetaArchivos",
				carpetaArchivos.resolve("no-existe").toString());

		assertThat(backupService.crearRespaldoArchivos()).isNull();
	}

	@Test
	@DisplayName("Elimina los respaldos que superan el tiempo de conservación")
	void eliminaRespaldosAntiguos() throws Exception {
		Files.writeString(carpetaArchivos.resolve("algo.txt"), "contenido");

		// Respaldo viejo (30 días) y uno reciente: solo debe sobrevivir el reciente.
		Path viejo = carpetaRespaldos.resolve("casaKetteler_2020-01-01_0200.sql");
		Files.writeString(viejo, "respaldo antiguo");
		viejo.toFile().setLastModified(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30));

		Path reciente = carpetaRespaldos.resolve("casaKetteler_hoy.sql");
		Files.writeString(reciente, "respaldo reciente");

		backupService.crearRespaldoArchivos(); // dispara la limpieza

		assertThat(Files.exists(viejo)).isFalse();
		assertThat(Files.exists(reciente)).isTrue();
	}
}
