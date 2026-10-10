package com.app.nutrimeta.data.local

import android.content.Context
import android.util.Log
import kotlin.math.abs
object BaseDatosInicializador {

    private const val TAG = "NutriMetaDB"

    fun iniciar(
        context: Context,
        firebaseUid: String
    ) {

        val appContext = context.applicationContext

        Thread {

            try {

                NutriMetaDatabaseHelper.forUser(
                    appContext,
                    firebaseUid
                ).use { helper ->

                    val db = helper.writableDatabase

                    // 1. VERIFICAR TABLAS

                    db.rawQuery(
                        """
                        SELECT name
                        FROM sqlite_master
                        WHERE type = 'table'
                        AND name NOT LIKE 'sqlite_%'
                        ORDER BY name
                        """.trimIndent(),
                        null
                    ).use { cursor ->

                        val nombres = mutableListOf<String>()

                        while (cursor.moveToNext()) {
                            nombres.add(cursor.getString(0))
                        }

                        Log.i(
                            TAG,
                            "SQLite preparada: ${nombres.joinToString()}"
                        )
                    }

                    // 2. VERIFICAR NIVELES DE ACTIVIDAD

                    val niveles = mutableListOf<String>()

                    db.rawQuery(
                        """
                        SELECT codigo, factor
                        FROM nivel_actividad
                        ORDER BY orden
                        """.trimIndent(),
                        null
                    ).use { cursor ->

                        while (cursor.moveToNext()) {

                            val codigo = cursor.getString(0)
                            val factor = cursor.getDouble(1)

                            niveles.add("$codigo = $factor")
                        }
                    }

                    Log.i(
                        TAG,
                        "Niveles de actividad encontrados: ${niveles.size}"
                    )

                    niveles.forEach { nivel ->

                        Log.i(TAG, nivel)
                    }

                    if (niveles.size != 5) {

                        Log.w(
                            TAG,
                            "Se esperaban 5 niveles de actividad, " +
                                    "pero se encontraron ${niveles.size}"
                        )
                    }


                    db.rawQuery(
                        """
                        SELECT
                        objetivo,
                        meta_kcal,
                        meta_proteina_g,
                        meta_grasa_g,
                        meta_carbohidratos_g
                        FROM perfil
                        WHERE id = 1
                        """.trimIndent(),
                        null
                    ).use { cursor ->

                        if (cursor.moveToFirst()) {

                            val objetivo = cursor.getString(0)
                            val calorias = cursor.getDouble(1)
                            val proteinas = cursor.getDouble(2)
                            val grasas = cursor.getDouble(3)
                            val carbohidratos = cursor.getDouble(4)

                            val caloriasMacros =
                                proteinas * 4.0 +
                                        grasas * 9.0 +
                                        carbohidratos * 4.0

                            Log.i(TAG, "===== PERFIL NUTRICIONAL =====")
                            Log.i(TAG, "Objetivo: $objetivo")
                            Log.i(TAG, "Calorías: $calorias kcal")
                            Log.i(TAG, "Proteínas: $proteinas g")
                            Log.i(TAG, "Grasas: $grasas g")
                            Log.i(TAG, "Carbohidratos: $carbohidratos g")
                            Log.i(TAG, "Calorías calculadas desde macros: $caloriasMacros")

                            val diferencia = abs(
                                calorias - caloriasMacros
                            )

                            Log.i(
                                TAG,
                                "¿Cuadran las calorías? ${diferencia < 0.01}"
                            )

                        } else {

                            Log.w(
                                TAG,
                                "Esta cuenta todavía no tiene perfil nutricional"
                            )
                        }
                    }
                }

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Error al preparar la base SQLite",
                    e
                )
            }

        }.start()
    }
}