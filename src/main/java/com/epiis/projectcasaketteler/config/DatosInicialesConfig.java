package com.epiis.projectcasaketteler.config;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.epiis.projectcasaketteler.entity.EntityAdmin;
import com.epiis.projectcasaketteler.entity.EntityAdmin.AdminRole;
import com.epiis.projectcasaketteler.entity.EntityResidence;
import com.epiis.projectcasaketteler.helper.PasswordEncoderHelper;
import com.epiis.projectcasaketteler.repository.RepositoryAdmin;
import com.epiis.projectcasaketteler.repository.RepositoryResidence;

/**
 * Deja el sistema utilizable en una instalación nueva.
 *
 * <p>
 * Sin esto, una instalación recién hecha arranca con la base de datos vacía: hay
 * tablas, pero ninguna cuenta con la que entrar, y como los residentes solo puede
 * crearlos un administrador, el sistema queda inservible.
 *
 * <p>
 * Solo actúa cuando <b>no existe ningún administrador</b>. En cuanto hay uno, no
 * vuelve a tocar nada: no pisa datos de una instalación en marcha ni recrea cuentas
 * que alguien haya borrado a propósito.
 */
@Component
public class DatosInicialesConfig implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosInicialesConfig.class);

    private static final String RESIDENCIA_POR_DEFECTO = "Casa Ketteler";

    @Autowired
    private RepositoryAdmin repositoryAdmin;

    @Autowired
    private RepositoryResidence repositoryResidence;

    @Autowired
    private PasswordEncoderHelper passwordEncoderHelper;

    // Se leen directamente las variables de entorno que escribe el instalador en
    // el .env, sin pasar por application.properties. Ese archivo no viaja en el
    // repositorio, y si faltara, el mapeo se perdería y la contraseña generada
    // para esta instalación se ignoraría en silencio, dejando la de por defecto.

    /** Correo del administrador inicial. */
    @Value("${ADMIN_INICIAL_EMAIL:admin@casaketteler.local}")
    private String emailInicial;

    /**
     * Contraseña del administrador inicial. El instalador genera una distinta para
     * cada instalación; este valor solo se usa si nadie la definió, y el sistema
     * avisa por registro de que hay que cambiarla.
     */
    @Value("${ADMIN_INICIAL_PASSWORD:CasaKetteler2026}")
    private String passwordInicial;

    @Override
    public void run(String... args) {
        if (repositoryAdmin.count() > 0) {
            return; // Ya hay administradores: no se toca nada.
        }

        EntityResidence residencia = obtenerOCrearResidencia();

        EntityAdmin admin = new EntityAdmin();
        admin.setIdAdmin(UUID.randomUUID().toString());
        admin.setParentResidence(residencia);
        admin.setFirstName("Administración");
        admin.setSurName(RESIDENCIA_POR_DEFECTO);
        admin.setEmail(emailInicial);
        admin.setPassword(passwordEncoderHelper.passwordEncoder().encode(passwordInicial));
        // SUPER_ADMIN y no ADMIN: es la única cuenta que existe, y hace falta ese
        // rol para registrar la residencia y dar de alta a otros administradores.
        admin.setRole(AdminRole.SUPER_ADMIN);
        admin.setActive(true);
        admin.setLoginAttempts(0);
        repositoryAdmin.save(admin);

        log.warn("");
        log.warn("===========================================================");
        log.warn("  PRIMER ARRANQUE: se creo la cuenta de administracion");
        log.warn("===========================================================");
        log.warn("  Residencia : {}", residencia.getName());
        log.warn("  Correo     : {}", emailInicial);
        log.warn("  Contrasena : {}", passwordInicial);
        log.warn("");
        log.warn("  CAMBIA ESTA CONTRASENA al entrar por primera vez.");
        log.warn("===========================================================");
        log.warn("");
    }

    /**
     * Usa la residencia que ya exista en la base de datos; si no hay ninguna, crea
     * la de Casa Ketteler. El SSID y el BSSID se dejan vacíos a propósito: los
     * registra el instalador o la administración, según la red de cada sede.
     */
    private EntityResidence obtenerOCrearResidencia() {
        List<EntityResidence> existentes = repositoryResidence.findAll();
        if (!existentes.isEmpty()) {
            EntityResidence residencia = existentes.get(0);
            log.info("Se usara la residencia ya registrada: {}", residencia.getName());
            return residencia;
        }

        EntityResidence residencia = new EntityResidence();
        residencia.setIdResidence(UUID.randomUUID().toString());
        residencia.setName(RESIDENCIA_POR_DEFECTO);
        residencia.setCreated_at(new Date());
        residencia.setUpdated_at(new Date());
        repositoryResidence.save(residencia);
        log.info("Se creo la residencia por defecto: {}", RESIDENCIA_POR_DEFECTO);
        return residencia;
    }
}
