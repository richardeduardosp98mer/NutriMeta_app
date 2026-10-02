-- =====================================================================
-- NutriMeta - Modelo físico (MySQL 8)
-- Versión 0.1 (propuesta) - 01-oct-2026
-- Contenido: estructura de tablas, vista de consumo diario,
--            datos de referencia y administrador inicial.
-- Los 200 alimentos se cargan aparte: nutrimeta_mysql_alimentos.sql
-- =====================================================================

DROP DATABASE IF EXISTS nutrimeta;
CREATE DATABASE nutrimeta CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE nutrimeta;

-- ---------------------------------------------------------------------
-- 1. TABLAS DE REFERENCIA (las administra el portal; la app las descarga)
--    Borrado físico solo si no están en uso (las FK lo impiden).
-- ---------------------------------------------------------------------

CREATE TABLE grupo_alimento (
    id_grupo        TINYINT UNSIGNED NOT NULL AUTO_INCREMENT,
    codigo          CHAR(1)          NOT NULL COMMENT 'Letra del grupo en las Tablas del INS',
    nombre          VARCHAR(60)      NOT NULL,
    PRIMARY KEY (id_grupo),
    UNIQUE KEY uk_grupo_codigo (codigo),
    UNIQUE KEY uk_grupo_nombre (nombre)
) ENGINE=InnoDB COMMENT='Grupos de alimentos según el INS';

CREATE TABLE nivel_actividad (
    id_nivel_actividad TINYINT UNSIGNED NOT NULL AUTO_INCREMENT,
    codigo          VARCHAR(20)      NOT NULL,
    nombre          VARCHAR(40)      NOT NULL,
    descripcion     VARCHAR(150)     NOT NULL,
    factor          DECIMAL(4,3)     NOT NULL COMMENT 'Multiplica la TMB para obtener el GET',
    orden           TINYINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (id_nivel_actividad),
    UNIQUE KEY uk_nivel_codigo (codigo),
    CONSTRAINT ck_nivel_factor CHECK (factor BETWEEN 1.000 AND 2.500)
) ENGINE=InnoDB COMMENT='Niveles de actividad física y su factor';

CREATE TABLE tipo_comida (
    id_tipo_comida  TINYINT UNSIGNED NOT NULL AUTO_INCREMENT,
    nombre          VARCHAR(30)      NOT NULL,
    orden           TINYINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (id_tipo_comida),
    UNIQUE KEY uk_tipo_comida_nombre (nombre)
) ENGINE=InnoDB COMMENT='Desayuno, almuerzo, cena, etc.';

-- ---------------------------------------------------------------------
-- 2. CATÁLOGO DE ALIMENTOS
--    Nunca se borra físicamente: se desactiva (activo = 0).
-- ---------------------------------------------------------------------

CREATE TABLE alimento (
    id_alimento          INT UNSIGNED     NOT NULL AUTO_INCREMENT,
    codigo_ins           VARCHAR(8)       NULL COMMENT 'Código del INS, ej. "A 3". NULL si lo creó el administrador',
    id_grupo             TINYINT UNSIGNED NOT NULL,
    nombre               VARCHAR(150)     NOT NULL,
    energia_kcal         DECIMAL(6,1)     NOT NULL COMMENT 'Por 100 g de parte comestible',
    proteina_g           DECIMAL(6,2)     NOT NULL COMMENT 'Por 100 g',
    grasa_g              DECIMAL(6,2)     NOT NULL COMMENT 'Por 100 g',
    carbohidratos_g      DECIMAL(6,2)     NOT NULL COMMENT 'Por 100 g, según metodo_carbohidratos',
    fibra_g              DECIMAL(6,2)     NULL     COMMENT 'Por 100 g. NULL = no reportado por el INS (no es 0)',
    metodo_carbohidratos ENUM('DISPONIBLES','TOTALES_MENOS_FIBRA','TOTALES','DERIVADO_ENERGIA','MANUAL')
                                          NOT NULL COMMENT 'Cómo se obtuvo carbohidratos_g (regla del 30-sep-2026)',
    activo               TINYINT(1)       NOT NULL DEFAULT 1,
    fecha_creacion       DATETIME(6)      NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    fecha_actualizacion  DATETIME(6)      NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
                                          COMMENT 'Habilita la futura actualización incremental del catálogo',
    PRIMARY KEY (id_alimento),
    UNIQUE KEY uk_alimento_codigo_ins (codigo_ins),
    KEY ix_alimento_grupo (id_grupo),
    KEY ix_alimento_nombre (nombre),
    KEY ix_alimento_actualizacion (fecha_actualizacion),
    CONSTRAINT fk_alimento_grupo FOREIGN KEY (id_grupo) REFERENCES grupo_alimento (id_grupo),
    CONSTRAINT ck_alimento_valores CHECK (
        energia_kcal >= 0 AND proteina_g >= 0 AND grasa_g >= 0 AND carbohidratos_g >= 0
        AND (fibra_g IS NULL OR fibra_g >= 0)
        AND proteina_g + grasa_g + carbohidratos_g <= 100
    )
) ENGINE=InnoDB COMMENT='Catálogo de alimentos (Tablas Peruanas de Composición de Alimentos, INS 11.ª ed. 2023)';

-- ---------------------------------------------------------------------
-- 3. USUARIOS DE LA APP Y SU PERFIL
--    Identidad en Firebase Authentication; datos en MySQL (unidos por firebase_uid).
-- ---------------------------------------------------------------------

CREATE TABLE usuario (
    id_usuario                 INT UNSIGNED  NOT NULL AUTO_INCREMENT,
    firebase_uid               VARCHAR(128)  NOT NULL,
    correo                     VARCHAR(254)  NOT NULL COMMENT 'Informativo; la fuente de verdad es Firebase',
    nombre                     VARCHAR(100)  NOT NULL,
    activo                     TINYINT(1)    NOT NULL DEFAULT 1,
    fecha_aceptacion_terminos  DATETIME      NOT NULL COMMENT 'Consentimiento: advertencia de uso + aviso de privacidad (Ley 29733)',
    version_terminos           VARCHAR(10)   NOT NULL DEFAULT '1.0',
    fcm_token                  VARCHAR(255)  NULL     COMMENT 'Token de notificaciones push del celular del usuario',
    fecha_creacion             DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    fecha_actualizacion        DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id_usuario),
    UNIQUE KEY uk_usuario_firebase_uid (firebase_uid),
    KEY ix_usuario_correo (correo)
) ENGINE=InnoDB COMMENT='Usuarios de la app móvil';

CREATE TABLE perfil (
    id_perfil              INT UNSIGNED     NOT NULL AUTO_INCREMENT,
    id_usuario             INT UNSIGNED     NOT NULL,
    sexo                   ENUM('M','F')    NOT NULL COMMENT 'Necesario para Mifflin-St Jeor',
    fecha_nacimiento       DATE             NOT NULL,
    peso_kg                DECIMAL(5,2)     NOT NULL,
    talla_cm               DECIMAL(5,1)     NOT NULL,
    id_nivel_actividad     TINYINT UNSIGNED NOT NULL,
    objetivo               ENUM('BAJAR','MANTENER','SUBIR') NOT NULL,
    peso_objetivo_kg       DECIMAL(5,2)     NULL COMMENT 'NULL si objetivo = MANTENER',
    plazo_semanas          SMALLINT UNSIGNED NULL COMMENT 'NULL si objetivo = MANTENER',
    meta_kcal              DECIMAL(6,1)     NOT NULL,
    meta_proteina_g        DECIMAL(6,1)     NOT NULL,
    meta_grasa_g           DECIMAL(6,1)     NOT NULL,
    meta_carbohidratos_g   DECIMAL(6,1)     NOT NULL,
    meta_ajustada          TINYINT(1)       NOT NULL DEFAULT 0 COMMENT '1 = la meta pedida se ajustó por seguridad',
    version                INT UNSIGNED     NOT NULL DEFAULT 1 COMMENT 'Solo la incrementa el servidor',
    fecha_actualizacion    DATETIME(6)      NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id_perfil),
    UNIQUE KEY uk_perfil_usuario (id_usuario),
    KEY ix_perfil_actualizacion (fecha_actualizacion),
    CONSTRAINT fk_perfil_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario),
    CONSTRAINT fk_perfil_nivel FOREIGN KEY (id_nivel_actividad) REFERENCES nivel_actividad (id_nivel_actividad),
    CONSTRAINT ck_perfil_rangos CHECK (peso_kg BETWEEN 30 AND 300 AND talla_cm BETWEEN 120 AND 230),
    CONSTRAINT ck_perfil_objetivo CHECK (
        (objetivo = 'MANTENER' AND peso_objetivo_kg IS NULL AND plazo_semanas IS NULL)
        OR (objetivo = 'BAJAR' AND peso_objetivo_kg < peso_kg AND plazo_semanas >= 1)
        OR (objetivo = 'SUBIR' AND peso_objetivo_kg > peso_kg AND plazo_semanas >= 1)
    )
) ENGINE=InnoDB COMMENT='Perfil físico, objetivo y meta vigente (1 por usuario). Se sincroniza en ambos sentidos';

-- ---------------------------------------------------------------------
-- 4. REGISTROS DE COMIDA (se sincronizan en ambos sentidos)
--    Borrado lógico (eliminado = 1) para que la eliminación llegue al celular.
-- ---------------------------------------------------------------------

CREATE TABLE registro_comida (
    id_registro          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    uuid                 CHAR(36)         NOT NULL COMMENT 'Lo genera la app; identifica el registro en celular y servidor',
    id_usuario           INT UNSIGNED     NOT NULL,
    id_tipo_comida       TINYINT UNSIGNED NOT NULL,
    fecha                DATE             NOT NULL COMMENT 'Día al que pertenece la comida',
    version              INT UNSIGNED     NOT NULL DEFAULT 1 COMMENT 'Solo la incrementa el servidor',
    eliminado            TINYINT(1)       NOT NULL DEFAULT 0,
    fecha_creacion       DATETIME(6)      NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    fecha_actualizacion  DATETIME(6)      NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id_registro),
    UNIQUE KEY uk_registro_uuid (uuid),
    KEY ix_registro_usuario_fecha (id_usuario, fecha),
    KEY ix_registro_usuario_actualizacion (id_usuario, fecha_actualizacion),
    CONSTRAINT fk_registro_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario),
    CONSTRAINT fk_registro_tipo FOREIGN KEY (id_tipo_comida) REFERENCES tipo_comida (id_tipo_comida)
) ENGINE=InnoDB COMMENT='Una comida registrada (cabecera)';

CREATE TABLE detalle_registro (
    id_detalle        BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_registro       BIGINT UNSIGNED NOT NULL,
    id_alimento       INT UNSIGNED    NOT NULL,
    cantidad_g        DECIMAL(7,2)    NOT NULL,
    energia_kcal      DECIMAL(8,2)    NOT NULL COMMENT 'Congelado al registrar (RN-07)',
    proteina_g        DECIMAL(8,2)    NOT NULL COMMENT 'Congelado al registrar',
    grasa_g           DECIMAL(8,2)    NOT NULL COMMENT 'Congelado al registrar',
    carbohidratos_g   DECIMAL(8,2)    NOT NULL COMMENT 'Congelado al registrar',
    PRIMARY KEY (id_detalle),
    KEY ix_detalle_registro (id_registro),
    KEY ix_detalle_alimento (id_alimento),
    CONSTRAINT fk_detalle_registro FOREIGN KEY (id_registro) REFERENCES registro_comida (id_registro) ON DELETE CASCADE,
    CONSTRAINT fk_detalle_alimento FOREIGN KEY (id_alimento) REFERENCES alimento (id_alimento),
    CONSTRAINT ck_detalle_cantidad CHECK (cantidad_g > 0 AND cantidad_g <= 2000),
    CONSTRAINT ck_detalle_valores CHECK (energia_kcal >= 0 AND proteina_g >= 0 AND grasa_g >= 0 AND carbohidratos_g >= 0)
) ENGINE=InnoDB COMMENT='Ingredientes de un registro, con sus valores congelados';

-- ---------------------------------------------------------------------
-- 5. ADMINISTRADORES DEL PORTAL (login propio con Spring Security)
-- ---------------------------------------------------------------------

CREATE TABLE administrador (
    id_administrador  INT UNSIGNED NOT NULL AUTO_INCREMENT,
    correo            VARCHAR(254) NOT NULL,
    nombre            VARCHAR(100) NOT NULL,
    password_hash     VARCHAR(100) NOT NULL COMMENT 'BCrypt',
    activo            TINYINT(1)   NOT NULL DEFAULT 1,
    fecha_creacion    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id_administrador),
    UNIQUE KEY uk_administrador_correo (correo)
) ENGINE=InnoDB COMMENT='Usuarios del portal administrativo';

-- ---------------------------------------------------------------------
-- 6. VISTA: consumo diario calculado por consulta (RN-08)
--    No es una tabla: no guarda datos ni requiere mantenimiento.
-- ---------------------------------------------------------------------

CREATE VIEW vw_consumo_diario AS
SELECT r.id_usuario,
       r.fecha,
       COUNT(DISTINCT r.id_registro)   AS registros,
       ROUND(SUM(d.energia_kcal), 1)    AS energia_kcal,
       ROUND(SUM(d.proteina_g), 1)      AS proteina_g,
       ROUND(SUM(d.grasa_g), 1)         AS grasa_g,
       ROUND(SUM(d.carbohidratos_g), 1) AS carbohidratos_g
FROM registro_comida r
JOIN detalle_registro d ON d.id_registro = r.id_registro
WHERE r.eliminado = 0
GROUP BY r.id_usuario, r.fecha;

-- ---------------------------------------------------------------------
-- 7. DATOS DE REFERENCIA
-- ---------------------------------------------------------------------

-- Los 8 grupos priorizados + 3 complementarios propuestos (D, J, K; ver modelo-datos-nutrimeta.md, decisión M1)
INSERT INTO grupo_alimento (codigo, nombre) VALUES
 ('A','Cereales y derivados'),
 ('B','Verduras, hortalizas y derivados'),
 ('C','Frutas y derivados'),
 ('D','Grasas, aceites y oleaginosas'),
 ('E','Pescados y mariscos'),
 ('F','Carnes y derivados'),
 ('G','Leches y derivados'),
 ('J','Huevos y derivados'),
 ('K','Productos azucarados'),
 ('T','Leguminosas y derivados'),
 ('U','Tubérculos, raíces y derivados');

-- Factores de actividad estándar usados con Mifflin-St Jeor
INSERT INTO nivel_actividad (codigo, nombre, descripcion, factor, orden) VALUES
 ('SEDENTARIO','Sedentario','Poco o nada de ejercicio',1.200,1),
 ('LIGERO','Ligero','Ejercicio ligero 1 a 3 días por semana',1.375,2),
 ('MODERADO','Moderado','Ejercicio moderado 3 a 5 días por semana',1.550,3),
 ('ACTIVO','Activo','Ejercicio intenso 6 a 7 días por semana',1.725,4),
 ('MUY_ACTIVO','Muy activo','Ejercicio muy intenso y trabajo físico',1.900,5);

INSERT INTO tipo_comida (nombre, orden) VALUES
 ('Desayuno',1),
 ('Media mañana',2),
 ('Almuerzo',3),
 ('Lonche',4),
 ('Cena',5);

-- Administrador inicial. Contraseña: NutriMeta2026!  (CAMBIARLA desde el portal tras el primer ingreso)
INSERT INTO administrador (correo, nombre, password_hash) VALUES
 ('admin@nutrimeta.pe','Administrador NutriMeta','$2a$10$dsGtv05avZLf5UN58mruYe3NDMyU0MAylmEcq.pvK0qCc02ndrm3m');
