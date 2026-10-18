package com.app.nutrimeta.data.local

import android.content.ContentValues
import android.content.Context
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

                db.setTransactionSuccessful()

            } finally {
                db.endTransaction()
            }
        }
    }

    private fun redondear(valor: Double): Double {
        return Math.round(valor * 100.0) / 100.0
    }
}
