package com.epiis.projectcasaketteler;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
		// `mvn` no lee el .env; en producción estos valores vienen de variables de
		// entorno. Aquí se dan valores dummy SOLO para que el contexto de Spring
		// cargue durante el test contextLoads() y `mvn package` no falle.
		"app.storage.path=./target/test-storage",
		"app.temp.path=./target/test-temp",
		"jwt.secret=test-secret-solo-para-cargar-el-contexto-en-tests-0123456789",
		"spring.mail.username=test@example.com",
		"spring.mail.password=test",
})
class ProjectcasakettelerApplicationTests {

	@Test
	void contextLoads() {
	}

}
