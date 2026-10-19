package com.app.nutrimeta.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.Serializable
import java.util.UUID

// Un ingrediente del plato: el alimento y su cantidad (en g o ml, segun la
// unidad del alimento). Los valores del catalogo son por 100 g o 100 ml.
data class IngredientePlato(
    val alimento: AlimentoDb,
    val cantidad: Double
) : Serializable {

    val energiaKcal: Double get() = alimento.energiaKcal * cantidad / 100.0
    val proteinaG: Double get() = alimento.proteinaG * cantidad / 100.0
    val grasaG: Double get() = alimento.grasaG * cantidad / 100.0
    val carbohidratosG: Double get() = alimento.carbohidratosG * cantidad / 100.0
}

// Resumen de un plato ya guardado, para la lista del historial.
data class ResumenPlato(
    val idRegistro: Long,
    val nombresIngredientes: List<String>,
    val kcal: Double
)

class RegistroComidaRepositorio(
    private val context: Context
) {

    companion object {
        const val CANTIDAD_MAXIMA = 2000.0
    }

    // Guarda un plato completo dentro de una comida del dia indicado
    // (fecha: "yyyy-MM-dd"): un registro_comida y un detalle_registro por
    // ingrediente, todo en una sola transaccion (o se guarda todo o nada).
    // version, eliminado y pendiente toman los valores por defecto de la
    // tabla (pendiente = 1: se sube en la proxima sincronizacion).
    // Cada detalle guarda los macros ya calculados para su cantidad, por lo
    // que no cambian si el catalogo se actualiza despues.
    fun guardarPlato(
        firebaseUid: String,
        idTipoComida: Int,
        fecha: String,
        ingredientes: List<IngredientePlato>
    ) {

        validar(ingredientes)

        NutriMetaDatabaseHelper.forUser(context, firebaseUid).use { helper ->

            val db = helper.writableDatabase

            db.beginTransaction()

            try {

                val registro = ContentValues().apply {
                    put("uuid", UUID.randomUUID().toString())
                    put("id_tipo_comida", idTipoComida)
                    put("fecha", fecha)
                }

                val idRegistro = db.insertOrThrow(
                    "registro_comida",
                    null,
                    registro
                )

                insertarDetalles(db, idRegistro, ingredientes)

                db.setTransactionSuccessful()

            } finally {
                db.endTransaction()
            }
        }
    }

    // Platos guardados (no eliminados) de una comida y un dia, en orden de creacion.
    fun listarPlatos(
        firebaseUid: String,
        idTipoComida: Int,
        fecha: String
    ): List<ResumenPlato> {

        NutriMetaDatabaseHelper.forUser(context, firebaseUid).use { helper ->

            val nombres = LinkedHashMap<Long, MutableList<String>>()
            val calorias = HashMap<Long, Double>()

            helper.readableDatabase.rawQuery(
                """
                SELECT r.id_registro, a.nombre, d.energia_kcal
                FROM registro_comida r
                JOIN detalle_registro d ON d.id_registro = r.id_registro
                JOIN alimento a ON a.id_alimento = d.id_alimento
                WHERE r.eliminado = 0
                    AND r.id_tipo_comida = ?
                    AND SUBSTR(r.fecha, 1, 10) = ?
                ORDER BY r.id_registro, d.id_detalle
                """.trimIndent(),
                arrayOf(idTipoComida.toString(), fecha)
            ).use { cursor ->

                while (cursor.moveToNext()) {

                    val id = cursor.getLong(0)

                    nombres.getOrPut(id) { mutableListOf() }.add(cursor.getString(1))
                    calorias[id] = (calorias[id] ?: 0.0) + cursor.getDouble(2)
                }
            }

            return nombres.map { (id, lista) ->
                ResumenPlato(id, lista, calorias[id] ?: 0.0)
            }
        }
    }

    // Ingredientes de un plato guardado, con el alimento actual del catalogo.
    fun cargarIngredientes(
        firebaseUid: String,
        idRegistro: Long
    ): List<IngredientePlato> {

        val filas = mutableListOf<Pair<Long, Double>>()

        NutriMetaDatabaseHelper.forUser(context, firebaseUid).use { helper ->

            helper.readableDatabase.rawQuery(
                """
                SELECT d.id_alimento, d.cantidad_g
                FROM detalle_registro d
                JOIN registro_comida r ON r.id_registro = d.id_registro
                WHERE d.id_registro = ? AND r.eliminado = 0
                ORDER BY d.id_detalle
                """.trimIndent(),
                arrayOf(idRegistro.toString())
            ).use { cursor ->

                while (cursor.moveToNext()) {
                    filas.add(Pair(cursor.getLong(0), cursor.getDouble(1)))
                }
            }
        }

        val alimentos = AlimentoRepositorio(context)

        return filas.mapNotNull { (idAlimento, cantidad) ->
            alimentos.obtenerPorId(firebaseUid, idAlimento)
                ?.let { IngredientePlato(it, cantidad) }
        }
    }

    // Reemplaza los ingredientes de un plato guardado. Sube la version y lo marca
    // pendiente para que la proxima sincronizacion envie el cambio.
    fun actualizarPlato(
        firebaseUid: String,
        idRegistro: Long,
        ingredientes: List<IngredientePlato>
    ) {

        validar(ingredientes)

        NutriMetaDatabaseHelper.forUser(context, firebaseUid).use { helper ->

            val db = helper.writableDatabase

            db.beginTransaction()

            try {

                db.delete(
                    "detalle_registro",
                    "id_registro = ?",
                    arrayOf(idRegistro.toString())
                )

                insertarDetalles(db, idRegistro, ingredientes)

                marcarCambio(db, idRegistro, eliminar = false)

                db.setTransactionSuccessful()

            } finally {
                db.endTransaction()
            }
        }
    }

    // Borrado logico: el registro queda eliminado y pendiente; al confirmarse en
    // el servidor, la sincronizacion lo borra del celular.
    fun eliminarPlato(firebaseUid: String, idRegistro: Long) {

        NutriMetaDatabaseHelper.forUser(context, firebaseUid).use { helper ->

            val db = helper.writableDatabase

            db.beginTransaction()

            try {

                marcarCambio(db, idRegistro, eliminar = true)

                db.setTransactionSuccessful()

            } finally {
                db.endTransaction()
            }
        }
    }

    private fun marcarCambio(db: SQLiteDatabase, idRegistro: Long, eliminar: Boolean) {

        db.compileStatement(
            """
            UPDATE registro_comida
            SET version = version + 1,
                pendiente = 1,
                eliminado = ?
            WHERE id_registro = ? AND eliminado = 0
            """.trimIndent()
        ).use { sentencia ->

            sentencia.bindLong(1, if (eliminar) 1L else 0L)
            sentencia.bindLong(2, idRegistro)

            check(sentencia.executeUpdateDelete() == 1) {
                "El plato ya no existe"
            }
        }
    }

    private fun validar(ingredientes: List<IngredientePlato>) {

        require(ingredientes.isNotEmpty()) {
            "El plato no tiene ingredientes"
        }

        for (ingrediente in ingredientes) {
            require(
                ingrediente.cantidad > 0.0 &&
                        ingrediente.cantidad <= CANTIDAD_MAXIMA
            ) {
                "Cantidad fuera de rango"
            }
        }
    }

    private fun insertarDetalles(
        db: SQLiteDatabase,
        idRegistro: Long,
        ingredientes: List<IngredientePlato>
    ) {

        for (ingrediente in ingredientes) {

            val detalle = ContentValues().apply {
                put("id_registro", idRegistro)
                put("id_alimento", ingrediente.alimento.id)
                put("cantidad_g", ingrediente.cantidad)
                put("energia_kcal", redondear(ingrediente.energiaKcal))
                put("proteina_g", redondear(ingrediente.proteinaG))
                put("grasa_g", redondear(ingrediente.grasaG))
                put("carbohidratos_g", redondear(ingrediente.carbohidratosG))
            }

            db.insertOrThrow("detalle_registro", null, detalle)
        }
    }

    private fun redondear(valor: Double): Double {
        return Math.round(valor * 100.0) / 100.0
    }
}
