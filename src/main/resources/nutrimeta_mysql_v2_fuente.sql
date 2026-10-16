-- =====================================================================
-- NutriMeta - Marca el origen de los alimentos del seed (idempotente).
-- Ejecutar DESPUÉS de nutrimeta_mysql_alimentos.sql, en BD nueva o migrada.
-- Los alimentos creados desde el portal quedan con el valor por defecto 'ADMIN'.
-- =====================================================================
USE nutrimeta;

UPDATE alimento SET fuente = 'INS' WHERE codigo_ins IS NOT NULL;

-- Valores imputados desde otras tablas (asterisco en el Excel del INS)
UPDATE alimento SET fuente = 'INS_IMPUTADO'
 WHERE codigo_ins IN ('A 142', 'F 66', 'F 78', 'F 92', 'G 21');
