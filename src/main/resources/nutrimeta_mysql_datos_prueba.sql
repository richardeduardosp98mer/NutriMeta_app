-- =====================================================================
-- NutriMeta · Datos de PRUEBA para el portal (solo desarrollo local)
-- Crea 2 usuarios, sus perfiles y 3 registros de comida con detalles,
-- para probar las pantallas Usuarios y Registros del portal.
--
-- Requisitos: esquema v2 + alimentos ya cargados.
-- Se puede ejecutar varias veces: borra primero sus propios datos de prueba
-- (usuarios con firebase_uid que empieza con 'prueba-').
-- NO ejecutar en la base compartida o de producción.
-- =====================================================================

USE nutrimeta;
SET SQL_SAFE_UPDATES = 0;

-- Limpieza de una ejecución anterior (el detalle se borra en cascada)
DELETE r FROM registro_comida r
  JOIN usuario u ON u.id_usuario = r.id_usuario
 WHERE u.firebase_uid LIKE 'prueba-%';
DELETE p FROM perfil p
  JOIN usuario u ON u.id_usuario = p.id_usuario
 WHERE u.firebase_uid LIKE 'prueba-%';
DELETE FROM usuario WHERE firebase_uid LIKE 'prueba-%';

-- Tres alimentos cualquiera del catálogo para armar los detalles
SET @a1 = (SELECT id_alimento FROM alimento WHERE activo = 1 ORDER BY id_alimento LIMIT 1 OFFSET 0);
SET @a2 = (SELECT id_alimento FROM alimento WHERE activo = 1 ORDER BY id_alimento LIMIT 1 OFFSET 1);
SET @a3 = (SELECT id_alimento FROM alimento WHERE activo = 1 ORDER BY id_alimento LIMIT 1 OFFSET 2);

-- ---------------------------------------------------------------------
-- Usuario 1: quiere bajar de peso
-- ---------------------------------------------------------------------
INSERT INTO usuario (firebase_uid, correo, nombre, fecha_aceptacion_terminos, version_terminos)
VALUES ('prueba-uid-1', 'ana.prueba@nutrimeta.test', 'Ana Prueba', NOW(), '1.0');
SET @u1 = LAST_INSERT_ID();

INSERT INTO perfil (id_usuario, sexo, fecha_nacimiento, peso_kg, talla_cm, id_nivel_actividad,
                    objetivo, peso_objetivo_kg, plazo_semanas,
                    meta_kcal, meta_proteina_g, meta_grasa_g, meta_carbohidratos_g, meta_ajustada)
VALUES (@u1, 'F', '1998-05-12', 70.00, 165.0,
        (SELECT id_nivel_actividad FROM nivel_actividad WHERE codigo = 'MODERADO'),
        'BAJAR', 65.00, 12,
        1900.0, 112.0, 53.0, 244.0, 0);   -- valores aproximados: se recalculan al guardar desde el portal

-- ---------------------------------------------------------------------
-- Usuario 2: mantiene su peso
-- ---------------------------------------------------------------------
INSERT INTO usuario (firebase_uid, correo, nombre, fecha_aceptacion_terminos, version_terminos)
VALUES ('prueba-uid-2', 'luis.prueba@nutrimeta.test', 'Luis Prueba', NOW(), '1.0');
SET @u2 = LAST_INSERT_ID();

INSERT INTO perfil (id_usuario, sexo, fecha_nacimiento, peso_kg, talla_cm, id_nivel_actividad,
                    objetivo, peso_objetivo_kg, plazo_semanas,
                    meta_kcal, meta_proteina_g, meta_grasa_g, meta_carbohidratos_g, meta_ajustada)
VALUES (@u2, 'M', '1995-03-20', 78.00, 176.0,
        (SELECT id_nivel_actividad FROM nivel_actividad WHERE codigo = 'LIGERO'),
        'MANTENER', NULL, NULL,
        2450.0, 125.0, 68.0, 334.0, 0);

-- ---------------------------------------------------------------------
-- Registros de comida (tipo_comida: 1 Desayuno, 2 Almuerzo, 3 Cena, 4 Snacks)
-- ---------------------------------------------------------------------
-- Usuario 1 · desayuno de hoy
INSERT INTO registro_comida (uuid, id_usuario, id_tipo_comida, fecha)
VALUES (UUID(), @u1, 1, CURDATE());
SET @r1 = LAST_INSERT_ID();

INSERT INTO detalle_registro (id_registro, id_alimento, cantidad_g,
                              energia_kcal, proteina_g, grasa_g, carbohidratos_g)
SELECT @r1, a.id_alimento, 120,
       ROUND(a.energia_kcal * 120 / 100, 2), ROUND(a.proteina_g * 120 / 100, 2),
       ROUND(a.grasa_g * 120 / 100, 2),      ROUND(a.carbohidratos_g * 120 / 100, 2)
  FROM alimento a WHERE a.id_alimento = @a1;
INSERT INTO detalle_registro (id_registro, id_alimento, cantidad_g,
                              energia_kcal, proteina_g, grasa_g, carbohidratos_g)
SELECT @r1, a.id_alimento, 80,
       ROUND(a.energia_kcal * 80 / 100, 2), ROUND(a.proteina_g * 80 / 100, 2),
       ROUND(a.grasa_g * 80 / 100, 2),      ROUND(a.carbohidratos_g * 80 / 100, 2)
  FROM alimento a WHERE a.id_alimento = @a2;

-- Usuario 1 · almuerzo de ayer
INSERT INTO registro_comida (uuid, id_usuario, id_tipo_comida, fecha)
VALUES (UUID(), @u1, 2, CURDATE() - INTERVAL 1 DAY);
SET @r2 = LAST_INSERT_ID();

INSERT INTO detalle_registro (id_registro, id_alimento, cantidad_g,
                              energia_kcal, proteina_g, grasa_g, carbohidratos_g)
SELECT @r2, a.id_alimento, 200,
       ROUND(a.energia_kcal * 200 / 100, 2), ROUND(a.proteina_g * 200 / 100, 2),
       ROUND(a.grasa_g * 200 / 100, 2),      ROUND(a.carbohidratos_g * 200 / 100, 2)
  FROM alimento a WHERE a.id_alimento = @a3;

-- Usuario 2 · cena de hoy
INSERT INTO registro_comida (uuid, id_usuario, id_tipo_comida, fecha)
VALUES (UUID(), @u2, 3, CURDATE());
SET @r3 = LAST_INSERT_ID();

INSERT INTO detalle_registro (id_registro, id_alimento, cantidad_g,
                              energia_kcal, proteina_g, grasa_g, carbohidratos_g)
SELECT @r3, a.id_alimento, 150,
       ROUND(a.energia_kcal * 150 / 100, 2), ROUND(a.proteina_g * 150 / 100, 2),
       ROUND(a.grasa_g * 150 / 100, 2),      ROUND(a.carbohidratos_g * 150 / 100, 2)
  FROM alimento a WHERE a.id_alimento = @a1;

SET SQL_SAFE_UPDATES = 1;

-- Verificación rápida
SELECT u.nombre, p.objetivo, COUNT(DISTINCT r.id_registro) AS registros, COUNT(d.id_detalle) AS detalles
  FROM usuario u
  JOIN perfil p ON p.id_usuario = u.id_usuario
  LEFT JOIN registro_comida r ON r.id_usuario = u.id_usuario
  LEFT JOIN detalle_registro d ON d.id_registro = r.id_registro
 WHERE u.firebase_uid LIKE 'prueba-%'
 GROUP BY u.id_usuario, u.nombre, p.objetivo;
