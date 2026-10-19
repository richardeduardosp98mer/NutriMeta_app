package com.app.nutrimeta.data.remote

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.app.nutrimeta.data.local.NutriMetaDatabaseHelper
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

// Sube al backend lo que esta pendiente en el celular (perfil y registros de comida)
// con POST /api/sync/subir, y marca como sincronizado lo que el servidor confirma.
// - Todo corre en un hilo aparte; el resultado se avisa en el hilo principal.
// - Solo se marca pendiente = 0 si la fila no cambio mientras viajaba (misma version).
// - El servidor responde por elemento: OK y RECHAZADO se dan por atendidos (RECHAZADO no se
//   reintenta); ERROR queda pendiente para el siguiente intento.
// - Sin red o con error del servidor no pasa nada: queda pendiente y se reintenta despues.
object SincronizadorSubida {

    private const val TAG = "SincronizadorSubida"
    private const val PREFS = "nutrimeta_sync"
    private const val MAX_REGISTROS_POR_ENVIO = 200
    private const val MAX_RONDAS = 5
    private const val ESPERA_TRAS_FALLO_MS = 20_000L

    enum class Resultado { SIN_PENDIENTES, ENVIADO, FALLO, DESACTIVADO, OCUPADO }

    private class PerfilPendiente(val version: Int, val json: JSONObject)

    private class RegistroPendiente(
        val uuid: String,
        val version: Int,
        val eliminado: Boolean,
        val json: JSONObject
    )

    private class Lote(val perfil: PerfilPendiente?, val registros: List<RegistroPendiente>) {
        val vacio: Boolean get() = perfil == null && registros.isEmpty()
    }

    private val enCurso = AtomicBoolean(false)
    private val ultimoFallo = AtomicLong(0L)
    private val principal = Handler(Looper.getMainLooper())

    // alDesactivado y alTerminar se ejecutan en el hilo principal.
    // manual = true: lo pidio el usuario (no se frena tras un fallo reciente).
    fun sincronizar(
        context: Context,
        usuario: FirebaseUser,
        manual: Boolean,
        alDesactivado: () -> Unit,
        alTerminar: (Resultado) -> Unit
    ) {

        val appContext = context.applicationContext
        val uid = usuario.uid

        if (!enCurso.compareAndSet(false, true)) {
            principal.post { alTerminar(Resultado.OCUPADO) }
            return
        }

        Thread {

            val resultado = try {
                trabajar(appContext, usuario, manual)
            } catch (error: Exception) {
                Log.w(TAG, "No se pudo sincronizar", error)
                Resultado.FALLO
            } finally {
                enCurso.set(false)
            }

            if (resultado == Resultado.FALLO) {
                ultimoFallo.set(SystemClock.elapsedRealtime())
            }

            principal.post {

                if (resultado == Resultado.DESACTIVADO &&
                    FirebaseAuth.getInstance().currentUser?.uid == uid
                ) {
                    alDesactivado()
                }

                alTerminar(resultado)
            }

        }.start()
    }

    // Cantidad de elementos (perfil y registros) que aun no se enviaron. Llamar fuera del hilo principal.
    fun contarPendientes(context: Context, uid: String): Int {

        return NutriMetaDatabaseHelper.forUser(context.applicationContext, uid).use { helper ->

            helper.readableDatabase.rawQuery(
                """
                SELECT (SELECT COUNT(*) FROM perfil WHERE pendiente = 1)
                     + (SELECT COUNT(*) FROM registro_comida WHERE pendiente = 1)
                """.trimIndent(),
                null
            ).use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(0) else 0
            }
        }
    }

    // Momento (milisegundos) de la ultima respuesta correcta del servidor; 0 si nunca hubo.
    fun ultimaSincronizacion(context: Context, uid: String): Long {
        return context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong("ultima_$uid", 0L)
    }

    private fun guardarUltimaSincronizacion(context: Context, uid: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong("ultima_$uid", System.currentTimeMillis())
            .apply()
    }

    private fun trabajar(context: Context, usuario: FirebaseUser, manual: Boolean): Resultado {

        val uid = usuario.uid

        if (contarPendientes(context, uid) == 0) {
            return Resultado.SIN_PENDIENTES
        }

        // Tras un fallo reciente, los intentos automaticos esperan un poco.
        if (!manual &&
            SystemClock.elapsedRealtime() - ultimoFallo.get() < ESPERA_TRAS_FALLO_MS
        ) {
            return Resultado.OCUPADO
        }

        val token = Tasks.await(usuario.getIdToken(false), 10, TimeUnit.SECONDS).token

        if (token.isNullOrBlank()) {
            return Resultado.FALLO
        }

        for (ronda in 1..MAX_RONDAS) {

            val lote = leerLote(context, uid)

            if (lote.vacio) {
                return Resultado.ENVIADO
            }

            val cuerpo = JSONObject().apply {
                lote.perfil?.let { put("perfil", it.json) }
                put("registros", JSONArray().apply {
                    lote.registros.forEach { put(it.json) }
                })
            }.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

            val respuesta = ApiCliente.sync.subir("Bearer $token", cuerpo).execute()

            val texto = (
                    if (respuesta.isSuccessful) respuesta.body()?.string()
                    else respuesta.errorBody()?.string()
                    ).orEmpty()

            if (respuesta.code() == 403 && texto.contains("usuario_inactivo")) {
                Log.w(TAG, "El backend indica que la cuenta esta desactivada")
                return Resultado.DESACTIVADO
            }

            if (respuesta.code() != 200) {
                Log.w(TAG, "El servidor respondio ${respuesta.code()}")
                return Resultado.FALLO
            }

            guardarUltimaSincronizacion(context, uid)

            val atendidos = aplicarRespuesta(context, uid, lote, JSONObject(texto))

            Log.i(TAG, "Ronda $ronda: $atendidos elementos atendidos por el servidor")

            // Si el servidor no atendio nada nuevo, no tiene sentido insistir.
            if (atendidos == 0) {
                return Resultado.FALLO
            }
        }

        return Resultado.ENVIADO
    }

    // --------------------------------------------------------------- lectura

    private fun leerLote(context: Context, uid: String): Lote {

        return NutriMetaDatabaseHelper.forUser(context, uid).use { helper ->

            val db = helper.readableDatabase

            Lote(leerPerfil(db), leerRegistros(db))
        }
    }

    private fun leerPerfil(db: SQLiteDatabase): PerfilPendiente? {

        return db.rawQuery(
            """
            SELECT p.sexo, p.fecha_nacimiento, p.peso_kg, p.talla_cm, n.codigo,
                   p.objetivo, p.peso_objetivo_kg, p.plazo_semanas,
                   p.meta_kcal, p.meta_proteina_g, p.meta_grasa_g, p.meta_carbohidratos_g,
                   p.meta_ajustada, p.version
            FROM perfil p
            JOIN nivel_actividad n ON n.id_nivel_actividad = p.id_nivel_actividad
            WHERE p.id = 1 AND p.pendiente = 1
            """.trimIndent(),
            null
        ).use { c ->

            if (!c.moveToFirst()) {
                null
            } else {

                val json = JSONObject().apply {
                    put("sexo", c.getString(0))
                    put("fechaNacimiento", c.getString(1))
                    put("pesoKg", c.getDouble(2))
                    put("tallaCm", c.getDouble(3))
                    put("codigoNivelActividad", c.getString(4))
                    put("objetivo", c.getString(5))
                    if (!c.isNull(6)) put("pesoObjetivoKg", c.getDouble(6))
                    if (!c.isNull(7)) put("plazoSemanas", c.getInt(7))
                    put("metaKcal", c.getDouble(8))
                    put("metaProteinaG", c.getDouble(9))
                    put("metaGrasaG", c.getDouble(10))
                    put("metaCarbohidratosG", c.getDouble(11))
                    put("metaAjustada", c.getInt(12) == 1)
                }

                PerfilPendiente(c.getInt(13), json)
            }
        }
    }

    private fun leerRegistros(db: SQLiteDatabase): List<RegistroPendiente> {

        val lista = ArrayList<RegistroPendiente>()

        db.rawQuery(
            """
            SELECT id_registro, uuid, id_tipo_comida, fecha, version, eliminado
            FROM registro_comida
            WHERE pendiente = 1
            ORDER BY id_registro
            LIMIT $MAX_REGISTROS_POR_ENVIO
            """.trimIndent(),
            null
        ).use { c ->

            while (c.moveToNext()) {

                val idRegistro = c.getLong(0)
                val uuid = c.getString(1)
                val version = c.getInt(4)
                val eliminado = c.getInt(5) == 1

                val json = JSONObject().apply {
                    put("uuid", uuid)
                    put("idTipoComida", c.getInt(2))
                    put("fecha", c.getString(3))
                    put("eliminado", eliminado)

                    if (!eliminado) {
                        put("detalles", leerDetalles(db, idRegistro))
                    }
                }

                lista.add(RegistroPendiente(uuid, version, eliminado, json))
            }
        }

        return lista
    }

    private fun leerDetalles(db: SQLiteDatabase, idRegistro: Long): JSONArray {

        val detalles = JSONArray()

        db.rawQuery(
            """
            SELECT id_alimento, cantidad_g, energia_kcal, proteina_g, grasa_g, carbohidratos_g
            FROM detalle_registro
            WHERE id_registro = ?
            ORDER BY id_detalle
            """.trimIndent(),
            arrayOf(idRegistro.toString())
        ).use { c ->

            while (c.moveToNext()) {
                detalles.put(
                    JSONObject().apply {
                        put("idAlimento", c.getLong(0))
                        put("cantidadG", c.getDouble(1))
                        put("energiaKcal", c.getDouble(2))
                        put("proteinaG", c.getDouble(3))
                        put("grasaG", c.getDouble(4))
                        put("carbohidratosG", c.getDouble(5))
                    }
                )
            }
        }

        return detalles
    }

    // ------------------------------------------------------------- respuesta

    // Marca lo atendido por el servidor. Devuelve cuantos elementos atendio.
    private fun aplicarRespuesta(
        context: Context,
        uid: String,
        lote: Lote,
        respuesta: JSONObject
    ): Int {

        var atendidos = 0

        NutriMetaDatabaseHelper.forUser(context, uid).use { helper ->

            val db = helper.writableDatabase

            db.beginTransaction()

            try {

                val resultadoPerfil = respuesta.optJSONObject("perfil")

                if (resultadoPerfil != null && lote.perfil != null &&
                    atendido(resultadoPerfil, "perfil")
                ) {
                    // Solo si el perfil no cambio mientras se enviaba.
                    db.execSQL(
                        "UPDATE perfil SET pendiente = 0 WHERE id = 1 AND version = ?",
                        arrayOf<Any?>(lote.perfil.version)
                    )
                    atendidos++
                }

                val porUuid = lote.registros.associateBy { it.uuid }
                val resultados = respuesta.optJSONArray("registros")

                if (resultados != null) {

                    for (i in 0 until resultados.length()) {

                        val item = resultados.optJSONObject(i) ?: continue
                        val uuid = item.optString("uuid")
                        val registro = porUuid[uuid] ?: continue

                        if (!atendido(item, uuid)) {
                            continue
                        }

                        if (registro.eliminado) {
                            // Borrado confirmado: se elimina la fila local (y su detalle en cascada).
                            db.execSQL(
                                "DELETE FROM registro_comida WHERE uuid = ? AND version = ? AND eliminado = 1",
                                arrayOf<Any?>(uuid, registro.version)
                            )
                        } else {
                            db.execSQL(
                                "UPDATE registro_comida SET pendiente = 0 WHERE uuid = ? AND version = ?",
                                arrayOf<Any?>(uuid, registro.version)
                            )
                        }

                        atendidos++
                    }
                }

                db.setTransactionSuccessful()

            } finally {
                db.endTransaction()
            }
        }

        return atendidos
    }

    // OK y RECHAZADO cierran el elemento; ERROR (o cualquier otro valor) lo deja pendiente.
    private fun atendido(item: JSONObject, nombre: String): Boolean {

        return when (item.optString("estado")) {
            "OK" -> true

            "RECHAZADO" -> {
                Log.w(TAG, "El servidor rechazo $nombre: ${item.optString("motivo")}")
                true
            }

            else -> false
        }
    }
}
