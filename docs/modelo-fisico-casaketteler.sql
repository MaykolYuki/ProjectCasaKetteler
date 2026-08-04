-- ============================================================================
--  CASA KETTELER — Modelo físico de la base de datos (MySQL 8)
-- ============================================================================
--  Refleja el DISEÑO del sistema, construido a partir de las entidades de la
--  aplicación (la fuente de la verdad). La base real la genera automáticamente
--  el ORM (Hibernate); este script es la versión limpia y documentada, apta para
--  importar en MySQL Workbench y generar el diagrama entidad-relación.
--
--  Uso:  mysql -u root -p < modelo-fisico-casaketteler.sql
--  O en Workbench:  File > Open SQL Script  y luego  Database > Reverse Engineer
-- ============================================================================

CREATE DATABASE IF NOT EXISTS casaKetteler
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE casaKetteler;

-- ----------------------------------------------------------------------------
--  tresidence — La residencia universitaria (entidad raíz)
-- ----------------------------------------------------------------------------
CREATE TABLE tresidence (
    idResidence   VARCHAR(36)   NOT NULL,
    name          VARCHAR(100),
    idAddress     VARCHAR(45),               -- dirección/IP de la residencia
    wifiSsid      VARCHAR(64),               -- nombre de la red Wi-Fi oficial
    wifiBssid     VARCHAR(64),               -- MAC del punto de acceso (validación de asistencia)
    created_at    DATETIME,
    updated_at    DATETIME,
    PRIMARY KEY (idResidence)
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
--  tadmin — Administradores (pertenecen a una residencia)
-- ----------------------------------------------------------------------------
CREATE TABLE tadmin (
    idAdmin         VARCHAR(36)  NOT NULL,
    idResidence     VARCHAR(36),                                  -- FK -> tresidence
    firstName       VARCHAR(100),
    surName         VARCHAR(100),
    email           VARCHAR(150),
    password        VARCHAR(255),                                 -- hash BCrypt
    role            ENUM('SUPER_ADMIN','ADMIN') DEFAULT 'ADMIN',
    active          TINYINT(1)   DEFAULT 1,
    loginAttempts   INT          DEFAULT 0,                       -- intentos fallidos (bloqueo)
    lockedUntil     DATETIME,                                     -- bloqueo temporal
    tokenValidAfter DATETIME,                                     -- sesión única
    created_at      DATETIME,
    updated_at      DATETIME,
    PRIMARY KEY (idAdmin),
    UNIQUE KEY uq_admin_email (email),
    CONSTRAINT fk_admin_residence FOREIGN KEY (idResidence)
        REFERENCES tresidence (idResidence) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
--  tuser — Residentes (pertenecen a una residencia)
-- ----------------------------------------------------------------------------
CREATE TABLE tuser (
    idUser             VARCHAR(36)  NOT NULL,
    idResidence        VARCHAR(36),                                     -- FK -> tresidence
    firstName          VARCHAR(100),
    surName            VARCHAR(100),
    email              VARCHAR(150),
    password           VARCHAR(255),                                    -- hash BCrypt
    cellPhoneNumber    VARCHAR(20),
    cellPhoneEmergency VARCHAR(20),
    ipAddressLocal     VARCHAR(45),
    role               ENUM('RESIDENTE','ADMIN','GUARDIA') DEFAULT 'RESIDENTE',
    active             TINYINT(1)   DEFAULT 1,
    firstLogin         TINYINT(1)   DEFAULT 1,                          -- exige cambio de contraseña inicial
    presente           TINYINT(1)   DEFAULT 1,                          -- dentro/fuera (define próxima marca)
    bestPhotoReference VARCHAR(255),                                    -- mejor foto para reconocimiento
    loginAttempts      INT          DEFAULT 0,
    lockedUntil        DATETIME,
    tokenValidAfter    DATETIME,
    created_at         DATETIME,
    updated_at         DATETIME,
    PRIMARY KEY (idUser),
    UNIQUE KEY uq_user_email (email),
    CONSTRAINT fk_user_residence FOREIGN KEY (idResidence)
        REFERENCES tresidence (idResidence) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
--  tphoto — Fotos de referencia para el reconocimiento facial
-- ----------------------------------------------------------------------------
CREATE TABLE tphoto (
    idPhoto        VARCHAR(36)  NOT NULL,
    iduser         VARCHAR(36),                    -- FK -> tuser
    namePhoto      VARCHAR(255),
    extensionPhoto VARCHAR(10),
    created_at     DATETIME,
    updated_at     DATETIME,
    PRIMARY KEY (idPhoto),
    CONSTRAINT fk_photo_user FOREIGN KEY (iduser)
        REFERENCES tuser (idUser) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
--  tattendance — Eventos de asistencia (un registro por cada marca)
-- ----------------------------------------------------------------------------
CREATE TABLE tattendance (
    idAtendance      VARCHAR(36)  NOT NULL,
    idUser           VARCHAR(36),                                         -- FK -> tuser
    eventTimestamp   DATETIME,
    eventType        ENUM('ENTRADA','SALIDA','INTENTO_FALLIDO'),
    esAnomalia       TINYINT(1)   DEFAULT 0,                              -- dos eventos iguales seguidos
    motivoFallo      VARCHAR(255),                                        -- si fue intento fallido
    description      VARCHAR(255),                                        -- motivo declarado por el residente
    ssid             VARCHAR(64),                                         -- red Wi-Fi al marcar
    bssid            VARCHAR(64),
    serverSimilarity DOUBLE,                                              -- % de coincidencia del rostro
    created_at       DATETIME,
    PRIMARY KEY (idAtendance),
    CONSTRAINT fk_attendance_user FOREIGN KEY (idUser)
        REFERENCES tuser (idUser) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
--  tdocumentGeneral — Documentos generales del residente (DNI, ficha, pagos...)
-- ----------------------------------------------------------------------------
CREATE TABLE tdocumentGeneral (
    idDocumentGeneral        VARCHAR(36)  NOT NULL,
    idUser                   VARCHAR(36),                                      -- FK -> tuser
    type                     VARCHAR(50),                                      -- DNI, FICHA, PAGO, NOTAS...
    nameDocumentGeneral      VARCHAR(255),
    extensionDocumentGeneral VARCHAR(10),
    period                   VARCHAR(10),                                      -- "2026-06" (mensual) / "2026-1" (semestral)
    status                   ENUM('PENDIENTE','APROBADO','OBSERVADO','RECHAZADO') DEFAULT 'PENDIENTE',
    observations             VARCHAR(500),
    downloadable             TINYINT(1)   DEFAULT 0,
    created_at               DATETIME,
    updated_at               DATETIME,
    PRIMARY KEY (idDocumentGeneral),
    CONSTRAINT fk_docgeneral_user FOREIGN KEY (idUser)
        REFERENCES tuser (idUser) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
--  tdocumentEnter — Documentos de entrada del residente
-- ----------------------------------------------------------------------------
CREATE TABLE tdocumentEnter (
    idDocumentEnter        VARCHAR(36)  NOT NULL,
    idUser                 VARCHAR(36),                                        -- FK -> tuser
    nameDocumentEnter      VARCHAR(255),
    extensionDocumentEnter VARCHAR(10),
    status                 ENUM('PENDIENTE','APROBADO','OBSERVADO','RECHAZADO') DEFAULT 'PENDIENTE',
    observations           VARCHAR(500),
    downloadable           TINYINT(1)   DEFAULT 0,
    created_at             DATETIME,
    updated_at             DATETIME,
    PRIMARY KEY (idDocumentEnter),
    CONSTRAINT fk_docenter_user FOREIGN KEY (idUser)
        REFERENCES tuser (idUser) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
--  tdocumentResignation — Carta de renuncia y su formato en blanco
-- ----------------------------------------------------------------------------
CREATE TABLE tdocumentResignation (
    idDocumentResignation      VARCHAR(36)  NOT NULL,
    idUser                     VARCHAR(36),                                    -- FK -> tuser
    nameDocumentResignation    VARCHAR(255),                                   -- carta firmada (sube el residente)
    extensionDocumentResignation VARCHAR(10),
    formatFileName             VARCHAR(255),                                   -- formato en blanco (asigna el admin)
    formatExtension            VARCHAR(10),
    formatAssignedAt           DATETIME,
    status                     ENUM('PENDIENTE','APROBADO','OBSERVADO','RECHAZADO') DEFAULT 'PENDIENTE',
    observations               VARCHAR(500),
    created_at                 DATETIME,
    updated_at                 DATETIME,
    PRIMARY KEY (idDocumentResignation),
    CONSTRAINT fk_docresignation_user FOREIGN KEY (idUser)
        REFERENCES tuser (idUser) ON DELETE CASCADE
) ENGINE=InnoDB;
