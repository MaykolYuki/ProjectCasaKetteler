package com.epiis.projectcasaketteler.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Permite que el backend sirva también la interfaz web (Angular), para que el
 * administrador entre desde el navegador de la PC sin necesidad de instalar Node
 * ni levantar un segundo servidor.
 *
 * Los archivos se leen de una carpeta EXTERNA (por defecto ./frontend, junto al JAR),
 * así se puede actualizar la interfaz sin volver a generar el JAR.
 *
 * Si la ruta pedida no es un archivo real (p. ej. /admin-home, que es una ruta interna
 * de Angular), se devuelve index.html para que el enrutador del front la resuelva.
 */
@Configuration
public class SpaConfig implements WebMvcConfigurer {

	@Value("${app.frontend.path:./frontend/}")
	private String frontendPath;

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		String ubicacion = frontendPath.endsWith("/") ? frontendPath : frontendPath + "/";

		registry.addResourceHandler("/**")
				.addResourceLocations("file:" + ubicacion)
				.resourceChain(true)
				.addResolver(new PathResourceResolver() {
					@Override
					protected Resource getResource(String resourcePath, Resource location) throws IOException {
						// Se sirve el archivo solo si la ruta apunta a un archivo real.
						// La raíz llega vacía, y rutas como /admin-home coinciden con
						// carpetas del build prerenderizado: en ambos casos debe ir el index.
						if (!resourcePath.isBlank()) {
							Resource pedido = location.createRelative(resourcePath);
							if (pedido.exists() && pedido.isReadable() && !esDirectorio(pedido)) {
								return pedido;
							}
						}
						Resource index = new FileSystemResource(ubicacion + "index.html");
						return index.exists() ? index : null;
					}

					private boolean esDirectorio(Resource recurso) {
						try {
							return recurso.getFile().isDirectory();
						} catch (IOException e) {
							return false;
						}
					}
				});
	}
}
