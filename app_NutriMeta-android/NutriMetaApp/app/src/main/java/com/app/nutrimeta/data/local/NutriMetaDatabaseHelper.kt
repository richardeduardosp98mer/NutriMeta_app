package com.app.nutrimeta.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.security.MessageDigest

class NutriMetaDatabaseHelper private constructor(
    context: Context,
    databaseName: String
) : SQLiteOpenHelper(context.applicationContext, databaseName, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_VERSION = 1

        // Requiere un UID autenticado; no usar correo ni contrasena.
        fun forUser(context: Context, firebaseUid: String): NutriMetaDatabaseHelper {
            require(firebaseUid.isNotBlank()) { "Firebase UID obligatorio" }
            val bytes = MessageDigest.getInstance("SHA-256")
                .digest(firebaseUid.toByteArray(Charsets.UTF_8))
            val hash = bytes.joinToString("") { "%02x".format(it.toInt() and 0xff) }
            val databaseName = "nutrimeta_${hash}.db"
            return NutriMetaDatabaseHelper(context, databaseName)
        }
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        // Activar las claves foraneas ANTES de usar la conexion.
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
                CREATE TABLE grupo_alimento (
                    id_grupo   INTEGER PRIMARY KEY,
                    codigo     TEXT    NOT NULL UNIQUE,
                    nombre     TEXT    NOT NULL
                );
            """.trimIndent()
        )
        db.execSQL(
            """
                CREATE TABLE nivel_actividad (
                    id_nivel_actividad INTEGER PRIMARY KEY,
                    codigo             TEXT    NOT NULL UNIQUE,
                    nombre             TEXT    NOT NULL,
                    descripcion        TEXT    NOT NULL,
                    factor             REAL    NOT NULL,
                    orden              INTEGER NOT NULL DEFAULT 0
                );
            """.trimIndent()
        )
        db.execSQL(
            """
                CREATE TABLE tipo_comida (
                    id_tipo_comida INTEGER PRIMARY KEY,
                    nombre         TEXT    NOT NULL UNIQUE,
                    orden          INTEGER NOT NULL DEFAULT 0
                );
            """.trimIndent()
        )
        db.execSQL(
            """
                CREATE TABLE alimento (
                    id_alimento      INTEGER PRIMARY KEY,
                    codigo_ins       TEXT,
                    id_grupo         INTEGER NOT NULL REFERENCES grupo_alimento (id_grupo),
                    nombre           TEXT    NOT NULL,
                    nombre_busqueda  TEXT    NOT NULL,
                    energia_kcal     REAL    NOT NULL,
                    proteina_g       REAL    NOT NULL,
                    grasa_g          REAL    NOT NULL,
                    carbohidratos_g  REAL    NOT NULL,
                    fibra_g          REAL,
                    activo           INTEGER NOT NULL DEFAULT 1 CHECK (activo IN (0,1))
                );
            """.trimIndent()
        )
        db.execSQL(
            """
                CREATE INDEX ix_alimento_busqueda ON alimento (nombre_busqueda);
            """.trimIndent()
        )
        db.execSQL(
            """
                CREATE TABLE perfil (
                    id                    INTEGER PRIMARY KEY CHECK (id = 1),
                    sexo                  TEXT    NOT NULL CHECK (sexo IN ('M','F')),
                    fecha_nacimiento      TEXT    NOT NULL,
                    peso_kg               REAL    NOT NULL,
                    talla_cm              REAL    NOT NULL,
                    id_nivel_actividad    INTEGER NOT NULL REFERENCES nivel_actividad (id_nivel_actividad),
                    objetivo              TEXT    NOT NULL CHECK (objetivo IN ('BAJAR','MANTENER','SUBIR')),
                    peso_objetivo_kg      REAL,
                    plazo_semanas         INTEGER,
                    meta_kcal             REAL    NOT NULL,
                    meta_proteina_g       REAL    NOT NULL,
                    meta_grasa_g          REAL    NOT NULL,
                    meta_carbohidratos_g  REAL    NOT NULL,
                    meta_ajustada         INTEGER NOT NULL DEFAULT 0 CHECK (meta_ajustada IN (0,1)),
                    version               INTEGER NOT NULL DEFAULT 0,
                    pendiente             INTEGER NOT NULL DEFAULT 1 CHECK (pendiente IN (0,1))
                );
            """.trimIndent()
        )
        db.execSQL(
            """
                CREATE TABLE registro_comida (
                    id_registro     INTEGER PRIMARY KEY AUTOINCREMENT,
                    uuid            TEXT    NOT NULL UNIQUE,
                    id_tipo_comida  INTEGER NOT NULL REFERENCES tipo_comida (id_tipo_comida),
                    fecha           TEXT    NOT NULL,
                    version         INTEGER NOT NULL DEFAULT 0,
                    eliminado       INTEGER NOT NULL DEFAULT 0 CHECK (eliminado IN (0,1)),
                    pendiente       INTEGER NOT NULL DEFAULT 1 CHECK (pendiente IN (0,1))
                );
            """.trimIndent()
        )
        db.execSQL(
            """
                CREATE INDEX ix_registro_fecha     ON registro_comida (fecha);
            """.trimIndent()
        )
        db.execSQL(
            """
                CREATE INDEX ix_registro_pendiente ON registro_comida (pendiente);
            """.trimIndent()
        )
        db.execSQL(
            """
                CREATE TABLE detalle_registro (
                    id_detalle       INTEGER PRIMARY KEY AUTOINCREMENT,
                    id_registro      INTEGER NOT NULL REFERENCES registro_comida (id_registro) ON DELETE CASCADE,
                    id_alimento      INTEGER NOT NULL REFERENCES alimento (id_alimento),
                    cantidad_g       REAL    NOT NULL CHECK (cantidad_g > 0 AND cantidad_g <= 2000),
                    energia_kcal     REAL    NOT NULL,
                    proteina_g       REAL    NOT NULL,
                    grasa_g          REAL    NOT NULL,
                    carbohidratos_g  REAL    NOT NULL
                );
            """.trimIndent()
        )
        db.execSQL(
            """
                CREATE INDEX ix_detalle_registro ON detalle_registro (id_registro);
            """.trimIndent()
        )
    }


    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)

        if (db.isReadOnly) return

        // Catálogo inicial de niveles de actividad.
        // INSERT OR IGNORE evita duplicar registros
        // y conserva los datos ya existentes.

        db.execSQL(
            """
        INSERT OR IGNORE INTO nivel_actividad
        (codigo, nombre, descripcion, factor, orden)
        VALUES
        (
            'SEDENTARIO',
            'Sedentario',
            'Poco o ningún ejercicio',
            1.200,
            1
        ),
        (
            'LIGERO',
            'Ligeramente activo',
            'Ejercicio ligero de 1 a 3 días por semana',
            1.375,
            2
        ),
        (
            'MODERADO',
            'Moderadamente activo',
            'Ejercicio moderado de 3 a 5 días por semana',
            1.550,
            3
        ),
        (
            'MUY_ACTIVO',
            'Muy activo',
            'Ejercicio intenso frecuente',
            1.725,
            4
        ),
        (
            'EXTREMO',
            'Extremadamente activo',
            'Actividad física muy exigente',
            1.900,
            5
        );
        """.trimIndent()
        )
    }


    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // NO usar DROP TABLE: se perderian los registros sin sincronizar.
        // Agregar aqui las migraciones cuando el equipo cambie el esquema.
        throw IllegalStateException(
            "Falta una migracion SQLite de la version $oldVersion a $newVersion"
        )
    }
}