package com.app.nutrimeta.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import java.text.Normalizer
import java.util.Locale

data class AlimentoDb(
    val id: Long,
    val codigoIns: String?,
    val idGrupo: Int,
    val grupo: String,
    val nombre: String,
    val unidad: String,
    val energiaKcal: Double,
    val proteinaG: Double,
    val grasaG: Double,
    val carbohidratosG: Double,
    val fibraG: Double?,
    val metodoCarbohidratos: String,
    val fuente: String
) : java.io.Serializable

class AlimentoRepositorio(
    private val context: Context
) {

    companion object {

        private const val ARCHIVO_SEED = "catalogo_seed.json"
        private const val LIMITE_RESULTADOS = 300

        private val lock = Any()

        private const val SELECT_ALIMENTO = """
            SELECT
                a.id_alimento,
                a.codigo_ins,
                a.id_grupo,
                g.nombre,
                a.nombre,
                a.unidad,
                a.energia_kcal,
                a.proteina_g,
                a.grasa_g,
                a.carbohidratos_g,
                a.fibra_g,
                a.metodo_carbohidratos,
                a.fuente
            FROM alimento a
            JOIN grupo_alimento g ON g.id_grupo = a.id_grupo
        """

        // Minusculas y sin tildes: "Piña" -> "pina". Se usa al guardar
        // nombre_busqueda y tambien con lo que escribe el usuario.
        fun normalizar(texto: String): String {
            val descompuesto = Normalizer.normalize(texto, Normalizer.Form.NFD)

            return descompuesto
                .replace(Regex("\\p{Mn}+"), "")
                .lowercase(Locale.ROOT)
                .trim()
        }

        // Para que % y _ escritos por el usuario se busquen como texto normal.
        private fun escaparLike(valor: String): String {
            return valor
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_")
        }
    }

    // ---------------------------------------------------------------
    // Consultas
    // ---------------------------------------------------------------

    // Busca alimentos activos. Cada palabra escrita debe coincidir con el
    // inicio de alguna palabra del nombre ("arroz coc" encuentra
    // "Arroz pilado o pulido cocido"; "pina" no encuentra "Espinaca").
    // Con texto vacio devuelve el catalogo ordenado por nombre.
    fun buscar(firebaseUid: String, texto: String): List<AlimentoDb> {

        asegurarCatalogoInicial(firebaseUid)

        val palabras = normalizar(texto)
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }

        val condiciones = StringBuilder("a.activo = 1")
        val argumentos = ArrayList<String>()

        for (palabra in palabras) {
            condiciones.append(
                " AND (a.nombre_busqueda LIKE ? ESCAPE '\\'" +
                        " OR a.nombre_busqueda LIKE ? ESCAPE '\\')"
            )
            // Al inicio del nombre, o despues de un espacio.
            argumentos.add(escaparLike(palabra) + "%")
            argumentos.add("% " + escaparLike(palabra) + "%")
        }

        // Primero los nombres que empiezan con la primera palabra.
        val orden = if (palabras.isEmpty()) {
            "a.nombre_busqueda"
        } else {
            argumentos.add(escaparLike(palabras[0]) + "%")
            "CASE WHEN a.nombre_busqueda LIKE ? ESCAPE '\\' THEN 0 ELSE 1 END, a.nombre_busqueda"
        }

        val sql = SELECT_ALIMENTO +
                " WHERE $condiciones" +
                " ORDER BY $orden" +
                " LIMIT $LIMITE_RESULTADOS"

        NutriMetaDatabaseHelper.forUser(context, firebaseUid).use { helper ->

            helper.readableDatabase.rawQuery(
                sql,
                argumentos.toTypedArray()
            ).use { cursor ->

                val lista = ArrayList<AlimentoDb>()

                while (cursor.moveToNext()) {
                    lista.add(leerAlimento(cursor))
                }

                return lista
            }
        }
    }

    fun obtenerPorId(firebaseUid: String, idAlimento: Long): AlimentoDb? {

        NutriMetaDatabaseHelper.forUser(context, firebaseUid).use { helper ->

            helper.readableDatabase.rawQuery(
                "$SELECT_ALIMENTO WHERE a.id_alimento = ?",
                arrayOf(idAlimento.toString())
            ).use { cursor ->

                return if (cursor.moveToFirst()) {
                    leerAlimento(cursor)
                } else {
                    null
                }
            }
        }
    }

    // Fecha para pedir solo lo nuevo: GET /api/catalogo?desde=<este valor>.
    // Es null mientras el catalogo viene solo del seed (se pide completo).
    fun ultimaSincronizacion(firebaseUid: String): String? {

        NutriMetaDatabaseHelper.forUser(context, firebaseUid).use { helper ->

            helper.readableDatabase.rawQuery(
                "SELECT MAX(fecha_actualizacion) FROM alimento",
                null
            ).use { cursor ->

                return if (cursor.moveToFirst() && !cursor.isNull(0)) {
                    cursor.getString(0)
                } else {
                    null
                }
            }
        }
    }

    // ---------------------------------------------------------------
    // Carga del catalogo
    // ---------------------------------------------------------------

    // Si la base del usuario no tiene alimentos, carga el catalogo que
    // viene en assets para poder buscar sin internet desde el primer uso.
    // Devuelve cuantos alimentos cargo (0 si ya habia datos).
    fun asegurarCatalogoInicial(firebaseUid: String): Int = synchronized(lock) {

        NutriMetaDatabaseHelper.forUser(context, firebaseUid).use { helper ->

            val db = helper.writableDatabase

            if (contarAlimentos(db) > 0) {
                0
            } else {
                val catalogo = CatalogoJson.parsear(leerSeed())

                // El seed no lleva fecha: asi la primera sincronizacion
                // con el servidor pide el catalogo completo y corrige
                // cualquier diferencia.
                guardarCatalogo(db, catalogo, sellarFecha = false)

                catalogo.alimentos.size
            }
        }
    }

    // Aplica una respuesta de GET /api/catalogo (para la sincronizacion).
    // Cada alimento recibido queda con fecha_actualizacion = sincronizadoEn,
    // por eso MAX(fecha_actualizacion) sirve como parametro "desde".
    fun aplicarCatalogo(firebaseUid: String, catalogo: CatalogoRespuesta): Int =
        synchronized(lock) {

            NutriMetaDatabaseHelper.forUser(context, firebaseUid).use { helper ->

                guardarCatalogo(
                    helper.writableDatabase,
                    catalogo,
                    sellarFecha = true
                )

                catalogo.alimentos.size
            }
        }

    private fun leerSeed(): String {
        return context.assets.open(ARCHIVO_SEED)
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
    }

    private fun contarAlimentos(db: SQLiteDatabase): Int {
        return db.rawQuery("SELECT COUNT(*) FROM alimento", null).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    // Inserta o actualiza, nunca borra. Primero UPDATE y, si no existia la
    // fila, INSERT. No se usa UPSERT (ON CONFLICT DO UPDATE) porque necesita
    // SQLite 3.24 y minSdk es 26; tampoco INSERT OR REPLACE, que borra la
    // fila y la vuelve a crear (perderia columnas no enviadas y arrastraria
    // detalle_registro si algun dia la clave foranea fuera ON DELETE CASCADE).
    private fun guardarCatalogo(
        db: SQLiteDatabase,
        catalogo: CatalogoRespuesta,
        sellarFecha: Boolean
    ) {

        val sello = if (sellarFecha) catalogo.sincronizadoEn else null

        db.beginTransaction()

        try {

            for (g in catalogo.grupos) {

                val valores = ContentValues().apply {
                    put("codigo", g.codigo)
                    put("nombre", g.nombre)
                }

                val filas = db.update(
                    "grupo_alimento",
                    valores,
                    "id_grupo = ?",
                    arrayOf(g.idGrupo.toString())
                )

                if (filas == 0) {
                    valores.put("id_grupo", g.idGrupo)
                    db.insertOrThrow("grupo_alimento", null, valores)
                }
            }

            for (a in catalogo.alimentos) {

                val valores = ContentValues().apply {
                    put("codigo_ins", a.codigoIns)
                    put("id_grupo", a.idGrupo)
                    put("nombre", a.nombre)
                    put("nombre_busqueda", normalizar(a.nombre))
                    put("unidad", a.unidad)
                    put("energia_kcal", a.energiaKcal)
                    put("proteina_g", a.proteinaG)
                    put("grasa_g", a.grasaG)
                    put("carbohidratos_g", a.carbohidratosG)
                    put("fibra_g", a.fibraG)
                    put("metodo_carbohidratos", a.metodoCarbohidratos)
                    put("fuente", a.fuente)
                    put("activo", if (a.activo) 1 else 0)
                    put("fecha_actualizacion", sello)
                }

                val filas = db.update(
                    "alimento",
                    valores,
                    "id_alimento = ?",
                    arrayOf(a.idAlimento.toString())
                )

                if (filas == 0) {
                    valores.put("id_alimento", a.idAlimento)
                    db.insertOrThrow("alimento", null, valores)
                }
            }

            db.setTransactionSuccessful()

        } finally {
            db.endTransaction()
        }
    }

    private fun leerAlimento(cursor: Cursor): AlimentoDb {
        return AlimentoDb(
            id = cursor.getLong(0),
            codigoIns = if (cursor.isNull(1)) null else cursor.getString(1),
            idGrupo = cursor.getInt(2),
            grupo = cursor.getString(3),
            nombre = cursor.getString(4),
            unidad = cursor.getString(5),
            energiaKcal = cursor.getDouble(6),
            proteinaG = cursor.getDouble(7),
            grasaG = cursor.getDouble(8),
            carbohidratosG = cursor.getDouble(9),
            fibraG = if (cursor.isNull(10)) null else cursor.getDouble(10),
            metodoCarbohidratos = cursor.getString(11),
            fuente = cursor.getString(12)
        )
    }
}
