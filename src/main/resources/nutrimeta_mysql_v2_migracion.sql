-- =====================================================================
-- NutriMeta - Migración MySQL v0.1 -> v0.2 (BD ya creada)
-- EJECUTAR UNA SOLA VEZ. Para una BD nueva usar nutrimeta_mysql.sql (ya en v0.2).
-- Antes de ejecutar, SELECT * FROM tipo_comida debe mostrar 5 filas:
-- 1 Desayuno, 2 Media mañana, 3 Almuerzo, 4 Lonche, 5 Cena.
-- =====================================================================
USE nutrimeta;

-- 1. alimento: unidad (g/ml) y fuente del dato -------------------------
ALTER TABLE alimento
    ADD COLUMN unidad ENUM('g','ml') NOT NULL DEFAULT 'g'
        COMMENT 'Unidad base: los valores nutricionales son por 100 de esta unidad y la porción se registra en ella'
        AFTER nombre,
    ADD COLUMN fuente ENUM('INS','INS_IMPUTADO','ETIQUETA','ADMIN') NOT NULL DEFAULT 'ADMIN'
        COMMENT 'Origen del dato: INS, INS con valor imputado (asterisco), etiqueta del producto o cargado por el administrador'
        AFTER metodo_carbohidratos;

ALTER TABLE detalle_registro
    MODIFY cantidad_g DECIMAL(7,2) NOT NULL COMMENT 'Cantidad en la unidad del alimento (g o ml)';

-- 2. Grupo H (bebidas) del INS ------------------------------------------
INSERT INTO grupo_alimento (codigo, nombre)
VALUES ('H', 'Bebidas (alcohólicas y analcohólicas)')
ON DUPLICATE KEY UPDATE nombre = nombre;

-- 3. Tipos de comida: 5 -> 4 (Desayuno, Almuerzo, Cena, Snacks) ----------
-- Ids finales: 1 Desayuno, 2 Almuerzo, 3 Cena, 4 Snacks (iguales en SQLite).
-- Media mañana (2) y Lonche (4) pasan a Snacks (4); Almuerzo 3->2; Cena 5->3.
UPDATE registro_comida
   SET id_tipo_comida = CASE id_tipo_comida
        WHEN 1 THEN 1 WHEN 2 THEN 4 WHEN 3 THEN 2 WHEN 4 THEN 4 WHEN 5 THEN 3
        ELSE id_tipo_comida END;
DELETE FROM tipo_comida WHERE id_tipo_comida = 5;
UPDATE tipo_comida SET nombre = 'Cena',     orden = 3 WHERE id_tipo_comida = 3;
UPDATE tipo_comida SET nombre = 'Almuerzo', orden = 2 WHERE id_tipo_comida = 2;
UPDATE tipo_comida SET nombre = 'Snacks',   orden = 4 WHERE id_tipo_comida = 4;
UPDATE tipo_comida SET orden = 1 WHERE id_tipo_comida = 1;

-- 4. Niveles de actividad: mismos códigos y nombres que la app -----------
-- (antes: 4 = ACTIVO, 5 = MUY_ACTIVO; la app usa MUY_ACTIVO y EXTREMO)
UPDATE nivel_actividad SET codigo = 'EXTREMO',    nombre = 'Extremadamente activo', descripcion = 'Actividad física muy exigente'                 WHERE id_nivel_actividad = 5;
UPDATE nivel_actividad SET codigo = 'MUY_ACTIVO', nombre = 'Muy activo',            descripcion = 'Ejercicio intenso frecuente'                 WHERE id_nivel_actividad = 4;
UPDATE nivel_actividad SET nombre = 'Moderadamente activo', descripcion = 'Ejercicio moderado de 3 a 5 días por semana' WHERE id_nivel_actividad = 3;
UPDATE nivel_actividad SET nombre = 'Ligeramente activo',   descripcion = 'Ejercicio ligero de 1 a 3 días por semana'   WHERE id_nivel_actividad = 2;
UPDATE nivel_actividad SET nombre = 'Sedentario',           descripcion = 'Poco o ningún ejercicio'                      WHERE id_nivel_actividad = 1;

-- Después de esto ejecutar nutrimeta_mysql_v2_fuente.sql
