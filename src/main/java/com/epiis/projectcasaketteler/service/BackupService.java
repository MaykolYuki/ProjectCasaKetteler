package com.epiis.projectcasaketteler.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Respaldo automático de la base de datos.
 *
 * Se ejecuta solo, mientras el sistema esté encendido, y conserva los últimos N días
 * para no llenar el disco. Usa mysqldump; la contraseña se pasa por variable de entorno
 * (MYSQL_PWD) para que no quede visible en la lista de procesos del sistema.
 *
 * OJO: esto respalda la BASE DE DATOS. Las fotos y documentos viven en la carpeta
 * de almacenamiento (app.storage.path) y deben copiarse aparte.
 */
@Service
public class BackupService {

	private static final Logger log = LoggerFactory.getLogger(BackupService.class);
	private static final DateTimeFormatter MARCA_TIEMPO = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm");

	@Value("${app.backup.enabled:true}")
	private boolean habilitado;

	@Value("${app.backup.path:./backups}")
	private String carpetaRespaldos;

	/** Ruta a mysqldump. En Windows no suele estar en el PATH. */
	@Value("${app.backup.mysqldump:C:/Program Files/MySQL/MySQL Server 8.0/bin/mysqldump.exe}")
	private String rutaMysqldump;

	@Value("${app.backup.retention-days:14}")
	private int diasQueSeConservan;

	@Value("${spring.datasource.username}")
	private String usuarioBd;

	@Value("${spring.datasource.password}")
	private String claveBd;

	@Value("${spring.datasource.url}")
	private String urlBd;

	/** Por defecto: todos los días a las 2:00 a. m. */
	@Scheduled(cron = "${app.backup.cron:0 0 2 * * *}")
	public void respaldoProgramado() {
		if (!habilitado) {
			return;
		}
		crearRespaldo();
	}

	/**
	 * Genera un respaldo ahora. Devuelve el archivo creado, o null si falló.
	 */
	public File crearRespaldo() {
		try {
			Path carpeta = Paths.get(carpetaRespaldos);
			Files.createDirectories(carpeta);

			String baseDatos = nombreBaseDatos();
			String nombre = baseDatos + "_" + LocalDateTime.now().format(MARCA_TIEMPO) + ".sql";
			File destino = carpeta.resolve(nombre).toFile();

			ProcessBuilder proceso = new ProcessBuilder(
					rutaMysqldump,
					"-u", usuarioBd,
					"--databases", baseDatos,
					"--single-transaction", // no bloquea la BD mientras respalda
					"--routines",
					"--events");
			// La contraseña por variable de entorno: no aparece en la lista de procesos.
			proceso.environment().put("MYSQL_PWD", claveBd);
			proceso.redirectOutput(destino);
			File errores = carpeta.resolve("ultimo-error.log").toFile();
			proceso.redirectError(errores);

			Process ejecucion = proceso.start();
			int codigo = ejecucion.waitFor();

			if (codigo != 0 || destino.length() == 0) {
				log.error("El respaldo de la base de datos falló (código {}). Revisa {}", codigo,
						errores.getAbsolutePath());
				Files.deleteIfExists(destino.toPath());
				return null;
			}

			log.info("Respaldo creado: {} ({} KB)", destino.getName(), destino.length() / 1024);
			borrarRespaldosAntiguos(carpeta);
			return destino;

		} catch (IOException e) {
			log.error("No se pudo crear el respaldo de la base de datos: {}", e.getMessage());
			return null;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("El respaldo fue interrumpido");
			return null;
		}
	}

	/** Conserva solo los respaldos de los últimos N días. */
	private void borrarRespaldosAntiguos(Path carpeta) {
		long limite = System.currentTimeMillis() - (diasQueSeConservan * 24L * 60 * 60 * 1000);

		try (Stream<Path> archivos = Files.list(carpeta)) {
			archivos.filter(p -> p.getFileName().toString().endsWith(".sql"))
					.filter(p -> p.toFile().lastModified() < limite)
					.sorted(Comparator.naturalOrder())
					.forEach(p -> {
						try {
							Files.delete(p);
							log.info("Respaldo antiguo eliminado: {}", p.getFileName());
						} catch (IOException e) {
							log.warn("No se pudo eliminar el respaldo {}", p.getFileName());
						}
					});
		} catch (IOException e) {
			log.warn("No se pudo revisar la carpeta de respaldos: {}", e.getMessage());
		}
	}

	/** Extrae el nombre de la BD desde la URL de conexión (jdbc:mysql://host:puerto/NOMBRE?params). */
	private String nombreBaseDatos() {
		String sinParametros = urlBd.split("\\?")[0];
		return sinParametros.substring(sinParametros.lastIndexOf('/') + 1);
	}
}
