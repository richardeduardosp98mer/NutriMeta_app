package com.app.nutrimeta.data.remote

import android.content.Context
import android.util.Log
import com.app.nutrimeta.data.local.AlimentoRepositorio
import com.app.nutrimeta.data.local.CatalogoJson
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

// Mantiene el catalogo de alimentos local al dia con GET /api/catalogo.
// Si no hay red o el backend no responde, no pasa nada: la app sigue
// usando el catalogo que ya tiene (el que viene en assets o el ultimo bajado).
object SincronizadorCatalogo {

    private const val TAG = "SincronizadorCatalogo"

    // Usuarios que ya intentaron sincronizar desde que arranco la app.
    private val yaIntentado = ConcurrentHashMap.newKeySet<String>()

    // Una vez por apertura de la app y por usuario, sin bloquear la pantalla.
    // Si falla, se reintenta la proxima vez que se abra la app.
    fun sincronizarAlAbrir(context: Context, firebaseUid: String) {

        if (!yaIntentado.add(firebaseUid)) {
            return
        }

        val appContext = context.applicationContext

        Thread {

            runCatching {
                sincronizar(appContext, firebaseUid)
            }.onSuccess { cambios ->
                Log.i(TAG, "Catalogo sincronizado: $cambios alimentos recibidos")
            }.onFailure { error ->
                Log.w(TAG, "No se pudo sincronizar el catalogo", error)
            }

        }.start()
    }

    // Pide solo lo nuevo (desde la ultima sincronizacion) y lo aplica.
    // Devuelve cuantos alimentos recibio.
    private fun sincronizar(context: Context, firebaseUid: String): Int {

        val repositorio = AlimentoRepositorio(context)

        val desde = repositorio.ultimaSincronizacion(firebaseUid)

        val respuesta = ApiCliente.catalogo.obtener(desde).execute()

        if (!respuesta.isSuccessful) {
            throw IOException("El servidor respondio ${respuesta.code()}")
        }

        val texto = respuesta.body()?.string()
            ?: throw IOException("Respuesta vacia")

        val catalogo = CatalogoJson.parsear(texto)

        return repositorio.aplicarCatalogo(firebaseUid, catalogo)
    }
}
