package com.app.nutrimeta.profile

import android.content.ContentValues
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.app.nutrimeta.data.local.NutriMetaDatabaseHelper
import com.app.nutrimeta.domain.CalculadoraMetas
import com.app.nutrimeta.domain.CalculadoraNutricional
import com.app.nutrimeta.domain.DatosCalculo
import com.app.nutrimeta.domain.ObjetivoNutricional
import com.app.nutrimeta.domain.SexoBiologico
import java.util.Locale

data class DatosRegistroPerfil(
    val objetivo: String,
    val sexo: String,
    val edad: Int,
    val fechaNacimiento: String,
    val altura: Int,
    val peso: Double,
    val actividad: String
)

object RegistroPerfilService {

    private const val TAG = "NutriMetaDB"

    fun guardarSiNoExiste(
        context: Context,
        firebaseUid: String,
        datos: DatosRegistroPerfil,
        alTerminar: (Result<Boolean>) -> Unit
    ) {
        val appContext = context.applicationContext

        Thread {
            val resultado = try {
                Result.success(
                    guardarEnSQLite(
                        appContext,
                        firebaseUid,
                        datos
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error al guardar perfil", e)
                Result.failure(e)
            }

            Handler(Looper.getMainLooper()).post {
                alTerminar(resultado)
            }
        }.start()
    }

    private fun guardarEnSQLite(
        context: Context,
        uid: String,
        datos: DatosRegistroPerfil
    ): Boolean {

        require(uid.isNotBlank()) {
            "UID obligatorio"
        }

        val sexo = when (
            datos.sexo.trim().lowercase(Locale.ROOT)
        ) {
            "hombre", "m" -> SexoBiologico.M
            "mujer", "f" -> SexoBiologico.F
            else -> error("Sexo no reconocido")
        }

        val objetivo = when (datos.objetivo) {
            "PERDER_GRASA" -> ObjetivoNutricional.BAJAR
            "MANTENER_PESO" -> ObjetivoNutricional.MANTENER
            "GANAR_MUSCULO" -> ObjetivoNutricional.SUBIR
            else -> error("Objetivo no reconocido")
        }

        require(datos.edad in 18..100) {
            "Edad no válida"
        }

        require(
            datos.fechaNacimiento.matches(
                Regex("""\d{4}-\d{2}-\d{2}""")
            )
        ) {
            "Fecha de nacimiento no válida"
        }

        require(datos.altura > 0) {
            "Altura no válida"
        }

        require(datos.peso.isFinite() && datos.peso > 0) {
            "Peso no válido"
        }

        val codigoActividad =
            datos.actividad.trim().uppercase(Locale.ROOT)

        NutriMetaDatabaseHelper.Companion.forUser(
            context,
            uid
        ).use { helper ->

            val db = helper.writableDatabase

            db.beginTransaction()

            try {
                // No modificar perfiles existentes.
                val existe = db.rawQuery(
                    "SELECT 1 FROM perfil WHERE id = 1",
                    null
                ).use { cursor ->
                    cursor.moveToFirst()
                }

                if (existe) {
                    db.setTransactionSuccessful()
                    return false
                }

                // Obtener ID y factor desde SQLite.
                val nivel = db.rawQuery(
                    """
                    SELECT id_nivel_actividad, factor
                    FROM nivel_actividad
                    WHERE codigo = ?
                    """.trimIndent(),
                    arrayOf(codigoActividad)
                ).use { cursor ->

                    check(cursor.moveToFirst()) {
                        "Nivel de actividad no encontrado"
                    }

                    Pair(
                        cursor.getInt(0),
                        cursor.getDouble(1)
                    )
                }

                // Calcular metabolismo y gasto diario.
                val energia = CalculadoraNutricional.calcular(
                    DatosCalculo(
                        sexo = sexo,
                        edad = datos.edad,
                        pesoKg = datos.peso,
                        tallaCm = datos.altura.toDouble(),
                        factorActividad = nivel.second
                    )
                )

                // Calcular calorías y macronutrientes.
                val metas = CalculadoraMetas.calcular(
                    gastoDiarioKcal = energia.gastoDiarioKcal,
                    pesoKg = datos.peso,
                    objetivo = objetivo
                )

                // Preparar perfil para SQLite.
                val valores = ContentValues().apply {
                    put("id", 1)
                    put("sexo", sexo.name)
                    put(
                        "fecha_nacimiento",
                        datos.fechaNacimiento
                    )
                    put("peso_kg", datos.peso)
                    put(
                        "talla_cm",
                        datos.altura.toDouble()
                    )
                    put(
                        "id_nivel_actividad",
                        nivel.first
                    )
                    put("objetivo", objetivo.name)

                    put("meta_kcal", metas.metaKcal)
                    put(
                        "meta_proteina_g",
                        metas.metaProteinaG
                    )
                    put(
                        "meta_grasa_g",
                        metas.metaGrasaG
                    )
                    put(
                        "meta_carbohidratos_g",
                        metas.metaCarbohidratosG
                    )
                }

                db.insertOrThrow(
                    "perfil",
                    null,
                    valores
                )

                db.setTransactionSuccessful()

                Log.i(
                    TAG,
                    "Perfil nutricional guardado correctamente"
                )

                return true

            } finally {
                db.endTransaction()
            }
        }
    }
}