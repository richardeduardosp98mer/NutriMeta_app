package com.app.nutrimeta.data.local

import android.content.Context

data class NivelActividadDb(
    val id: Int,
    val codigo: String,
    val factor: Double
)

class NivelActividadRepository(
    private val context: Context
) {

    fun obtenerPorCodigo(
        firebaseUid: String,
        codigo: String
    ): NivelActividadDb? {

        val helper = NutriMetaDatabaseHelper.forUser(
            context,
            firebaseUid
        )

        try {
            val db = helper.readableDatabase

            val cursor = db.query(
                "nivel_actividad",
                arrayOf(
                    "id_nivel_actividad",
                    "codigo",
                    "factor"
                ),
                "codigo = ?",
                arrayOf(codigo),
                null,
                null,
                null,
                "1"
            )

            cursor.use {

                if (!it.moveToFirst()) {
                    return null
                }

                val id = it.getInt(
                    it.getColumnIndexOrThrow(
                        "id_nivel_actividad"
                    )
                )

                val codigoEncontrado = it.getString(
                    it.getColumnIndexOrThrow("codigo")
                )

                val factor = it.getDouble(
                    it.getColumnIndexOrThrow("factor")
                )

                check(factor.isFinite() && factor > 0.0) {
                    "Factor de actividad inválido"
                }

                return NivelActividadDb(
                    id = id,
                    codigo = codigoEncontrado,
                    factor = factor
                )
            }

        } finally {
            helper.close()
        }
    }
}