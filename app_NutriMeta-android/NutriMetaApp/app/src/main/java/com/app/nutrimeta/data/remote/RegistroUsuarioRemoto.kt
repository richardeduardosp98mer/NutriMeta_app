package com.app.nutrimeta.data.remote

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

// Registra al usuario en el backend y comprueba que su cuenta siga activa
// (POST /api/usuarios/me con su ID token de Firebase).
// Se hace al abrir el dashboard, una vez por apertura de la app y por usuario
// (mientras el backend no haya respondido bien, se reintenta en cada apertura
// del dashboard). Al cerrar sesion se olvida lo confirmado, asi que cualquier
// inicio de sesion nuevo vuelve a preguntar al backend. Una cuenta desactivada
// en el portal queda bloqueada al iniciar sesion y en la proxima apertura de
// la app, siempre que haya internet.
// Si no hay red o el backend no responde, no pasa nada: la app sigue funcionando.
object RegistroUsuarioRemoto {

    private const val TAG = "RegistroUsuarioRemoto"
    private const val NOMBRE_MAXIMO = 100

    private enum class Resultado { REGISTRADO, DESACTIVADO, REINTENTAR }

    // Usuarios que el backend ya confirmo desde que arranco la app.
    private val confirmados = ConcurrentHashMap.newKeySet<String>()

    // Usuarios con una llamada en curso, para no lanzar dos a la vez.
    private val enCurso = ConcurrentHashMap.newKeySet<String>()

    private val principal = Handler(Looper.getMainLooper())

    // El oyente de sesion se registra una sola vez.
    private val escuchandoSesion = AtomicBoolean(false)

    // Cuando la sesion de Firebase se cierra, se olvida lo confirmado para que el
    // proximo inicio de sesion (aunque sea de la misma cuenta) se vuelva a verificar.
    private fun olvidarAlCerrarSesion() {

        if (!escuchandoSesion.compareAndSet(false, true)) {
            return
        }

        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            if (auth.currentUser == null) {
                confirmados.clear()
            }
        }
    }

    // alDesactivado se ejecuta en el hilo principal si el backend responde que
    // la cuenta esta desactivada (y el usuario sigue siendo el mismo).
    fun registrarAlAbrir(
        usuario: FirebaseUser,
        alDesactivado: () -> Unit
    ) {

        olvidarAlCerrarSesion()

        val uid = usuario.uid

        if (uid in confirmados) {
            return
        }

        if (!enCurso.add(uid)) {
            return
        }

        // Pide el token (Firebase lo renueva solo si ya vencio).
        usuario.getIdToken(false)
            .addOnSuccessListener { resultadoToken ->

                val token = resultadoToken.token

                if (token.isNullOrBlank()) {
                    enCurso.remove(uid)
                    return@addOnSuccessListener
                }

                Thread {
                    ejecutar(uid, token, usuario.displayName, alDesactivado)
                }.start()
            }
            .addOnFailureListener { error ->
                Log.w(TAG, "No se pudo obtener el token de Firebase", error)
                enCurso.remove(uid)
            }
    }

    private fun ejecutar(
        uid: String,
        token: String,
        nombre: String?,
        alDesactivado: () -> Unit
    ) {

        try {

            val resultado = runCatching {
                llamar(token, nombre)
            }.onFailure { error ->
                Log.w(TAG, "No se pudo registrar al usuario en el backend", error)
            }.getOrDefault(Resultado.REINTENTAR)

            when (resultado) {

                Resultado.REGISTRADO -> {
                    confirmados.add(uid)
                    Log.i(TAG, "Usuario confirmado por el backend")
                }

                Resultado.DESACTIVADO -> {
                    Log.w(TAG, "El backend indica que la cuenta esta desactivada")

                    principal.post {
                        if (FirebaseAuth.getInstance().currentUser?.uid == uid) {
                            alDesactivado()
                        }
                    }
                }

                Resultado.REINTENTAR -> Unit
            }

        } finally {
            enCurso.remove(uid)
        }
    }

    private fun llamar(token: String, nombre: String?): Resultado {

        // Solo se manda el nombre si lo hay; si no, el backend usa el correo.
        val json = JSONObject().apply {
            if (!nombre.isNullOrBlank()) {
                put("nombre", nombre.trim().take(NOMBRE_MAXIMO))
            }
        }.toString()

        val cuerpo = json.toRequestBody("application/json; charset=utf-8".toMediaType())

        val respuesta = ApiCliente.usuarios
            .registrarme("Bearer $token", cuerpo)
            .execute()

        val textoError = respuesta.errorBody()?.string().orEmpty()

        respuesta.body()?.close()

        return when {
            respuesta.code() == 200 || respuesta.code() == 201 -> Resultado.REGISTRADO

            // 403 solo se toma como desactivada si el backend lo dice expresamente.
            respuesta.code() == 403 && textoError.contains("usuario_inactivo") ->
                Resultado.DESACTIVADO

            else -> {
                Log.w(TAG, "El servidor respondio ${respuesta.code()}")
                Resultado.REINTENTAR
            }
        }
    }
}
