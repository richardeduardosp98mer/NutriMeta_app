-- NutriMeta - Carga de bebidas (grupo H del INS)
-- Fuente: INS, Tablas Peruanas de Composición de Alimentos, 11.ª ed. (2023), grupo H.
-- Los valores del INS son por 100 g; aquí se usan como por 100 ml (unidad = 'ml'), asumiendo densidad ≈ 1.
-- Se excluyen las bebidas alcohólicas (cerveza, vino blanco y vino tinto). Total: 29 bebidas.
-- Requiere el esquema v2 (columnas unidad y fuente, y el grupo H). Ejecutar después de nutrimeta_mysql_alimentos.sql, una sola vez.

USE nutrimeta;

INSERT INTO alimento (codigo_ins, id_grupo, nombre, unidad, energia_kcal, proteina_g, grasa_g, carbohidratos_g, fibra_g, metodo_carbohidratos, fuente) VALUES
('H 2', (SELECT id_grupo FROM grupo_alimento WHERE codigo = 'H'), 'Chicha de aguaje', 'ml', 54, 0.4, 0.8, 11.2, NULL, 'TOTALES', 'INS'),
('H 3', (SELECT id_grupo FROM grupo_alimento WHERE codigo = 'H'), 'Chicha de cebada', 'ml', 24, 0.1, 0.2, 5.5, NULL, 'TOTALES', 'INS'),
('H 4', (SELECT id_grupo FROM grupo_alimento WHERE codigo = 'H'), 'Chicha de jora', 'ml', 28, 0.4, 0.3, 5.8, NULL, 'TOTALES', 'INS'),
('H 5', (SELECT id_grupo FROM grupo_alimento WHERE codigo = 'H'), 'Chicha de maíz morado con azúcar', 'ml', 20, 0, 0, 4.9, NULL, 'TOTALES', 'INS'),
('H 6', (SELECT id_grupo FROM grupo_alimento WHERE codigo = 'H'), 'Chicha de maní', 'ml', 34, 0.5, 0.8, 6.3, NULL, 'TOTALES', 'INS'),
('H 7', (SELECT id_grupo FROM grupo_alimento WHERE codigo = 'H'), 'Chicha de pijuayo', 'ml', 47, 0.8, 0.1, 10.6, NULL, 'TOTALES', 'INS'),
('H 8', (SELECT id_grupo FROM grupo_alimento WHERE codigo = 'H'), 'Chicha de soya', 'ml', 41, 0.6, 0.2, 9.1, NULL, 'TOTALES', 'INS'),
('H 9', (SELECT id_grupo FROM grupo_alimento WHERE codigo = 'H'), 'Chicha de yuca (masato)', 'ml', 37, 0.2, 0.1, 8.9, NULL, 'TOTALES', 'INS'),
('H 10', (SELECT id_grupo FROM grupo_alimento WHERE codigo = 'H'), 'Coca Cola', 'ml', 39, 0, 0, 10.5, 0, 'DISPONIBLES', 'INS'),
('H 11', (SELECT id_grupo FROM grupo_alimento WHERE codigo = 'H'), 'Inca Kola', 'ml', 41, 0, 0, 11, NULL, 'TOTALES', 'INS'),
('H 12', (SELECT id_grupo FROM grupo_alimento WHERE codigo = 'H'), 'Bebida de soya', 'ml', 38, 3, 1.4, 2.5, 1.3, 'DISPONIBLES', 'INS'),
('H 13', (SELECT id_grupo FROM grupo_alimento WHERE codigo = 'H'), 'Té sin azúcar', 'ml', 1, 0.1, 0, 0.2, 0, 'DISPONIBLES', 'INS'),
('H 16', (SELECT id_grupo FROM grupo_alimento WHERE codigo = 'H'), 'Café sin azúcar', 'ml', 2, 0.1, 0, 0.6, 0, 'DISPONIBLES', 'INS'),
('H 17', (SELECT id_grupo FROM grupo_alimento WHERE codigo = 'H'), 'Ajonjolí, bebida de, natural, sin azúcar', 'ml', 46, 1.6, 4.2, 0.3, 3.1, 'DISPONIBLES', 'INS'),
('H 18', (SELECT id_grupo FROM grupo_alimento WHERE codigo = 'H'), 'Almendra, bebida de, natural, sin azúcar', 'ml', 73, 3, 6.4, 0.7, 4, 'DISPONIBLES', 'INS'),
('H 19', (SELECT id_grupo FROM grupo_alimento WHERE codigo = 'H'), 'Arándano y uva, bebida de, envasada', 'ml', 17, 0, 0, 4.3, NULL, 'TOTALES', 'INS'),
('H 20', (SELECT id_grupo FROM grupo_alimento WHERE codigo = 'H'), 'Avellana, bebida de, natural, sin azúcar', 'ml', 58, 1.4, 5.7, 0.3, 3.1, 'DISPONIBLES', 'INS'),
('H 21', (SELECT id_grupo FROM grupo_alimento WHERE codigo = 'H'), 'Carambola, refresco de, con azúcar', 'ml', 30, 0, 0, 7.3, 0.1, 'DISPONIBLES', 'INS'),
('H 22', (SELECT id_grupo FROM grupo_alimento WHERE codigo = 'H'), 'Cebada, agua de, natural, sin azúcar', 'ml', 3, 0.1, 0.1, 0.3, NULL, 'TOTALES', 'INS'),
('H 23', (SELECT id_grupo FROM grupo_alimento WHERE codigo = 'H'), 'Cebada, refresco de, con azúcar', 'ml', 22, 0.1, 0, 5.4, 0, 'DISPONIBLES', 'INS'),
('H 24', (SELECT id_grupo FROM grupo_alimento WHERE codigo = 'H'), 'Emoliente, con azúcar', 'ml', 15, 0.1, 0.3, 3, 0, 'DISPONIBLES', 'INS'),
('H 25', (SELECT id_grupo FROM grupo_alimento WHERE codigo = 'H'), 'Infusión de hierbas, con azúcar', 'ml', 28, 0, 0.2, 6.7, 0, 'DISPONIBLES', 'INS'),
('H 26', (SELECT id_grupo FROM grupo_alimento WHERE codigo = 'H'), 'Infusión de hierba luisa, con azúcar', 'ml', 14, 0, 0, 3.5, 0, 'DISPONIBLES', 'INS'),
('H 27', (SELECT id_grupo FROM grupo_alimento WHERE codigo = 'H'), 'Maíz morado, chicha de, natural, sin azúcar, sin limón', 'ml', 2, 0.1, 0.2, 0.2, NULL, 'TOTALES', 'INS'),
('H 28', (SELECT id_grupo FROM grupo_alimento WHERE codigo = 'H'), 'Malta, bebida de, sin alcohol, envasada', 'ml', 32, 0.3, 0, 7.6, NULL, 'TOTALES', 'INS'),
('H 29', (SELECT id_grupo FROM grupo_alimento WHERE codigo = 'H'), 'Manzana, refresco de, con azúcar', 'ml', 34, 0.1, 0.1, 8.2, 0, 'DISPONIBLES', 'INS'),
('H 30', (SELECT id_grupo FROM grupo_alimento WHERE codigo = 'H'), 'Maracuyá, refresco de, con azúcar', 'ml', 34, 0.1, 0.2, 7.9, 0, 'DISPONIBLES', 'INS'),
('H 31', (SELECT id_grupo FROM grupo_alimento WHERE codigo = 'H'), 'Naranja, mandarina y limón, bebida sabor a, envasada', 'ml', 12, 0, 0, 3.1, NULL, 'TOTALES', 'INS'),
('H 32', (SELECT id_grupo FROM grupo_alimento WHERE codigo = 'H'), 'Piña, néctar de, con edulcorante, envasada', 'ml', 15, 0.1, 0, 3.7, NULL, 'TOTALES', 'INS');
