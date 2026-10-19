package com.app.nutrimeta.data.remote

import android.content.ContentValues
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
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

// Repuebla el celular con lo que el servidor guarda del usuario (GET /api/sync/restaurar),
// pensado para una reinstalacion o un celular nuevo.
// - Solo actua si el celular NO tiene perfil local: nunca pisa datos que ya existen.
// - Todo se guarda en una sola transaccion: o se restaura todo o nada.
// - Lo restaurado queda como sincronizado (pendiente = 0), asi que no se vuelve a subir.
// - Si falta algun alimento en el catalogo local, se intenta bajar el catalogo; los platos
//   cuyos alimentos sigan sin existir se omiten (se registra en el log).
object SincronizadorRestauracion {

    private const val TAG = "SincronizadorRestaur"
    private const val PREFS = "nutrimeta_sync"
    private const val ESPERA_TRAS_FALLO_MS = 20_000L

    private val FECHA_ISO = Regex("""\d{4}-\d{2}-\d{2}""")

    enum class Resultado { NO_NECESARIO, RESTAURADO, SIN_DATOS, FALLO, DESACTIVADO, OCUPADO }

    private val enCurso = AtomicBoolean(false)
    private val ultimoFallo = AtomicLong(0L)
    private val principal = Handler(Looper.getMainLooper())

    // alEmpezar, alDesactivado y alTerminar se ejecutan en el hilo principal.
    // alEmpezar solo se llama si de verdad se va a pedir la restauracion al servidor.
    fun restaurarSiHaceFalta(
        context: Context,
        usuario: FirebaseUser,
        alEmpezar: () -> Unit,
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
                trabajar(appContext, usuario) {
                    principal.post { alEmpezar() }
                }
            } catch (error: Exception) {
                Log.w(TAG, "No se pudo restaurar", error)
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

    private fun trabajar(
        context: Context,
        usuario: FirebaseUser,
        alEmpezar: () -> Unit
    ): Resultado {

        val uid = usuario.uid

        if (hayPerfilLocal(context, uid)) {
            return Resultado.NO_NECESARIO
        }

        // Tras un fallo reciente se espera un poco antes de reintentar.
        if (SystemClock.elapsedRealtime() - ultimoFallo.get() < ESPERA_TRAS_FALLO_MS) {
            return Resultado.OCUPADO
        }

        alEmpezar()

        val token = Tasks.await(usuario.getIdToken(false), 10, TimeUnit.SECONDS).token

        if (token.isNullOrBlank()) {
            return Resultado.FALLO
        }

        val respuesta = ApiCliente.sync.restaurar("Bearer $token").execute()

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

        val cuerpo = JSONObject(texto)
        val perfil = cuerpo.optJSONObject("perfil") ?: return Resultado.SIN_DATOS
        val registros = cuerpo.optJSONArray("registros") ?: JSONArray()

        // Si el celular no tiene algun alimento de los platos, se intenta bajar el catalogo.
        if (hayAlimentosFaltantes(context, uid, registros)) {
            runCatching { SincronizadorCatalogo.sincronizar(context, uid) }
                .onFailure { Log.w(TAG, "No se pudo bajar el catalogo antes de restaurar", it) }
        }

        val guardados = guardar(context, uid, perfil, registros)

        if (guardados < 0) {
            // Mientras tanto se creo un perfil local: no se toca nada.
            return Resultado.NO_NECESARIO
        }

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong("ultima_$uid", System.currentTimeMillis())
            .apply()

        Log.i(TAG, "Datos restaurados: perfil y $guardados registros")

        return Resultado.RESTAURADO
    }

    private fun hayPerfilLocal(context: Context, uid: String): Boolean {

        return NutriMetaDatabaseHelper.forUser(context, uid).use { helper ->

            helper.readableDatabase.rawQuery(
                "SELECT 1 FROM perfil WHERE id = 1",
                null
            ).use { cursor -> cursor.moveToFirst() }
        }
    }

    private fun hayAlimentosFaltantes(
        context: Context,
        uid: String,
        registros: JSONArray
    ): Boolean {

        return NutriMetaDatabaseHelper.forUser(context, uid).use { helper ->

            val db = helper.readableDatabase

            (0 until registros.length()).any { i ->
                val detalles = registros.getJSONObject(i).getJSONArray("detalles")

                (0 until detalles.length()).any { j ->
                    !existeAlimento(db, detalles.getJSONObject(j).getLong("idAlimento"))
                }
            }
        }
    }

    private fun existeAlimento(db: SQLiteDatabase, idAlimento: Long): Boolean {

        return db.rawQuery(
            "SELECT 1 FROM alimento WHERE id_alimento = ?",
            arrayOf(idAlimento.toString())
        ).use { cursor -> cursor.moveToFirst() }
    }

    // Devuelve cuantos registros se guardaron, o -1 si ya habia un perfil local.
    private fun guardar(
        context: Context,
        uid: String,
        perfil: JSONObject,
        registros: JSONArray
    ): Int {

        return NutriMetaDatabaseHelper.forUser(context, uid).use { helper ->

            val db = helper.writableDatabase

            db.beginTransaction()

            try {

                val yaExiste = db.rawQuery(
                    "SELECT 1 FROM perfil WHERE id = 1",
                    null
                ).use { cursor -> cursor.moveToFirst() }

                if (yaExiste) {
                    db.setTransactionSuccessful()
                    return@use -1
                }

                guardarPerfil(db, perfil)

                var guardados = 0

                for (i in 0 until registros.length()) {
                    if (guardarRegistro(db, registros.getJSONObject(i))) {
                        guardados++
                    }
                }

                db.setTransactionSuccessful()

                guardados

            } finally {
                db.endTransaction()
            }
        }
    }

    private fun guardarPerfil(db: SQLiteDatabase, perfil: JSONObject) {

        val codigo = perfil.getString("codigoNivelActividad")

        val idNivel = db.rawQuery(
            "SELECT id_nivel_actividad FROM nivel_actividad WHERE codigo = ?",
            arrayOf(codigo)
        ).use { cursor ->

            check(cursor.moveToFirst()) {
                "Nivel de actividad no encontrado: $codigo"
            }

            cursor.getInt(0)
        }

        val fechaNacimiento = perfil.getString("fechaNacimiento")

        check(FECHA_ISO.matches(fechaNacimiento)) {
            "Fecha de nacimiento no valida: $fechaNacimiento"
        }

        val valores = ContentValues().apply {
            put("id", 1)
            put("sexo", perfil.getString("sexo"))
            put("fecha_nacimiento", fechaNacimiento)
            put("peso_kg", perfil.getDouble("pesoKg"))
            put("talla_cm", perfil.getDouble("tallaCm"))
            put("id_nivel_actividad", idNivel)
            put("objetivo", perfil.getString("objetivo"))

            if (!perfil.isNull("pesoObjetivoKg") && !perfil.isNull("plazoSemanas")) {
                put("peso_objetivo_kg", perfil.getDouble("pesoObjetivoKg"))
                put("plazo_semanas", perfil.getInt("plazoSemanas"))
            }

            put("meta_kcal", perfil.getDouble("metaKcal"))
            put("meta_proteina_g", perfil.getDouble("metaProteinaG"))
            put("meta_grasa_g", perfil.getDouble("metaGrasaG"))
            put("meta_carbohidratos_g", perfil.getDouble("metaCarbohidratosG"))
            put("meta_ajustada", if (perfil.optBoolean("metaAjustada", false)) 1 else 0)
            put("version", perfil.optInt("version", 1))
            put("pendiente", 0)
        }

        db.insertOrThrow("perfil", null, valores)
    }

    // Devuelve true si el registro se guardo; false si se omitio.
    private fun guardarRegistro(db: SQLiteDatabase, registro: JSONObject): Boolean {

        val uuid = registro.getString("uuid")
        val fecha = registro.getString("fecha")
        val detalles = registro.getJSONArray("detalles")

        if (!FECHA_ISO.matches(fecha) || detalles.length() == 0) {
            Log.w(TAG, "Registro $uuid omitido: datos no validos")
            return false
        }

        for (j in 0 until detalles.length()) {
            val idAlimento = detalles.getJSONObject(j).getLong("idAlimento")

            if (!existeAlimento(db, idAlimento)) {
                Log.w(TAG, "Registro $uuid omitido: falta el alimento $idAlimento en el catalogo local")
                return false
            }
        }

        val cabecera = ContentValues().apply {
            put("uuid", uuid)
            put("id_tipo_comida", registro.getInt("idTipoComida"))
            put("fecha", fecha)
            put("version", registro.optInt("version", 1))
            put("eliminado", 0)
            put("pendiente", 0)
        }

        // Si el uuid ya estaba en el celular, se conserva lo local.
        val idRegistro = db.insertWithOnConflict(
            "registro_comida",
            null,
            cabecera,
            SQLiteDatabase.CONFLICT_IGNORE
        )

        if (idRegistro == -1L) {
            return false
        }

        for (j in 0 until detalles.length()) {

            val d = detalles.getJSONObject(j)

            val detalle = ContentValues().apply {
                put("id_registro", idRegistro)
                put("id_alimento", d.getLong("idAlimento"))
                put("cantidad_g", d.getDouble("cantidadG"))
                put("energia_kcal", d.getDouble("energiaKcal"))
                put("proteina_g", d.getDouble("proteinaG"))
                put("grasa_g", d.getDouble("grasaG"))
                put("carbohidratos_g", d.getDouble("carbohidratosG"))
            }

            db.insertOrThrow("detalle_registro", null, detalle)
        }

        return true
    }
}
