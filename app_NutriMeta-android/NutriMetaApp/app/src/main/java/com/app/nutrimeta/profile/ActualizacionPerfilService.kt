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
import java.util.Calendar
import java.util.Locale

sealed class CambioPerfil {
    data class Objetivo(val codigo: String) : CambioPerfil()
    data class Peso(val kg: Double) : CambioPerfil()
    data class FechaNacimiento(val fecha: String) : CambioPerfil()
    data class Actividad(val codigo: String) : CambioPerfil()
}

object ActualizacionPerfilService {

    private const val TAG = "NutriMetaDB"
    private val mainHandler = Handler(Looper.getMainLooper())

    private data class PerfilActual(
        val sexo: String,
        val fechaNacimiento: String,
        val peso: Double,
        val altura: Double,
        val objetivo: String,
        val actividad: String,
        val version: Int
    )

    fun actualizar(
        context: Context,
        firebaseUid: String,
        cambio: CambioPerfil,
        alTerminar: (Result<Unit>) -> Unit
    ) {

        val appContext = context.applicationContext

        Thread {

            val resultado = runCatching {
                actualizarEnSQLite(
                    appContext,
                    firebaseUid,
                    cambio
                )
            }

            mainHandler.post {
                alTerminar(resultado)
            }

        }.start()
    }

    private fun actualizarEnSQLite(
        context: Context,
        uid: String,
        cambio: CambioPerfil
    ) {

        require(uid.isNotBlank()) {
            "UID obligatorio"
        }

        NutriMetaDatabaseHelper.Companion.forUser(
            context,
            uid
        ).use { helper ->

            val db = helper.writableDatabase

            db.beginTransaction()

            try {

                // Leer los datos actuales del perfil.
                val actual = db.rawQuery(
                    """
                    SELECT
                        p.sexo,
                        p.fecha_nacimiento,
                        p.peso_kg,
                        p.talla_cm,
                        p.objetivo,
                        n.codigo,
                        p.version
                    FROM perfil p
                    JOIN nivel_actividad n
                      ON n.id_nivel_actividad =
                         p.id_nivel_actividad
                    WHERE p.id = 1
                    """.trimIndent(),
                    null
                ).use { cursor ->

                    check(cursor.moveToFirst()) {
                        "No existe un perfil para actualizar"
                    }

                    PerfilActual(
                        sexo = cursor.getString(0),
                        fechaNacimiento = cursor.getString(1),
                        peso = cursor.getDouble(2),
                        altura = cursor.getDouble(3),
                        objetivo = cursor.getString(4),
                        actividad = cursor.getString(5),
                        version = cursor.getInt(6)
                    )
                }

                // Aplicar únicamente el dato modificado.
                val nuevaFecha =
                    if (cambio is CambioPerfil.FechaNacimiento) {
                        cambio.fecha
                    } else {
                        actual.fechaNacimiento
                    }

                val nuevoPeso =
                    if (cambio is CambioPerfil.Peso) {
                        cambio.kg
                    } else {
                        actual.peso
                    }

                val nuevoObjetivo =
                    if (cambio is CambioPerfil.Objetivo) {
                        cambio.codigo
                    } else {
                        actual.objetivo
                    }

                val nuevaActividad =
                    if (cambio is CambioPerfil.Actividad) {
                        cambio.codigo
                    } else {
                        actual.actividad
                    }

                val objetivo = when (nuevoObjetivo) {
                    "BAJAR", "PERDER_GRASA" ->
                        ObjetivoNutricional.BAJAR

                    "MANTENER", "MANTENER_PESO" ->
                        ObjetivoNutricional.MANTENER

                    "SUBIR", "GANAR_MUSCULO" ->
                        ObjetivoNutricional.SUBIR

                    else -> error("Objetivo no válido")
                }

                val sexo = when (actual.sexo) {
                    "M" -> SexoBiologico.M
                    "F" -> SexoBiologico.F
                    else -> error("Sexo no válido")
                }

                val edad = calcularEdad(nuevaFecha)

                require(nuevoPeso.isFinite() && nuevoPeso > 0) {
                    "Peso no válido"
                }

                require(actual.altura.isFinite() && actual.altura > 0) {
                    "Altura no válida"
                }

                // Consultar el factor real de SQLite.
                val nivel = db.rawQuery(
                    """
                    SELECT id_nivel_actividad, factor
                    FROM nivel_actividad
                    WHERE codigo = ?
                    """.trimIndent(),
                    arrayOf(
                        nuevaActividad.uppercase(Locale.ROOT)
                    )
                ).use { cursor ->

                    check(cursor.moveToFirst()) {
                        "Nivel de actividad no encontrado"
                    }

                    Pair(
                        cursor.getInt(0),
                        cursor.getDouble(1)
                    )
                }

                // Recalcular gasto energético.
                val energia = CalculadoraNutricional.calcular(
                    DatosCalculo(
                        sexo = sexo,
                        edad = edad,
                        pesoKg = nuevoPeso,
                        tallaCm = actual.altura,
                        factorActividad = nivel.second
                    )
                )

                // Recalcular metas nutricionales.
                val metas = CalculadoraMetas.calcular(
                    gastoDiarioKcal = energia.gastoDiarioKcal,
                    pesoKg = nuevoPeso,
                    objetivo = objetivo
                )

                // Actualizar la fila existente.
                val valores = ContentValues().apply {

                    put("fecha_nacimiento", nuevaFecha)
                    put("peso_kg", nuevoPeso)
                    put("objetivo", objetivo.name)

                    put(
                        "id_nivel_actividad",
                        nivel.first
                    )

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

                    put("meta_ajustada", 0)
                    put("pendiente", 1)
                    put("version", actual.version + 1)
                }

                val filas = db.update(
                    "perfil",
                    valores,
                    "id = ?",
                    arrayOf("1")
                )

                check(filas == 1) {
                    "No se pudo actualizar el perfil"
                }

                db.setTransactionSuccessful()

            } finally {
                db.endTransaction()
            }
        }

        Log.i(
            TAG,
            "Perfil actualizado y metas recalculadas"
        )
    }

    private fun calcularEdad(fechaISO: String): Int {

        require(
            fechaISO.matches(
                Regex("""\d{4}-\d{2}-\d{2}""")
            )
        ) {
            "Fecha de nacimiento inválida"
        }

        val partes = fechaISO.split("-")

        val nacimiento = Calendar.getInstance().apply {
            clear()
            isLenient = false
            set(
                partes[0].toInt(),
                partes[1].toInt() - 1,
                partes[2].toInt()
            )

            // Validar que sea una fecha real.
            timeInMillis
        }

        val hoy = Calendar.getInstance()

        var edad = hoy.get(Calendar.YEAR) -
                nacimiento.get(Calendar.YEAR)

        val mesActual = hoy.get(Calendar.MONTH)
        val mesNacimiento = nacimiento.get(Calendar.MONTH)

        val diaActual = hoy.get(Calendar.DAY_OF_MONTH)
        val diaNacimiento = nacimiento.get(Calendar.DAY_OF_MONTH)

        if (
            mesActual < mesNacimiento ||
            (
                    mesActual == mesNacimiento &&
                            diaActual < diaNacimiento
                    )
        ) {
            edad--
        }

        require(edad in 18..100) {
            "La edad debe estar entre 18 y 100 años"
        }

        return edad
    }
}