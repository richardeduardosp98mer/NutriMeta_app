package com.app.nutrimeta.data.remote

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface UsuarioApi {

    // Registra (o confirma) al usuario del token en el backend.
    // autorizacion: "Bearer <ID token de Firebase>".
    // El cuerpo es un JSON armado a mano; solo se mira el codigo de respuesta.
    @POST("api/usuarios/me")
    fun registrarme(
        @Header("Authorization") autorizacion: String,
        @Body cuerpo: RequestBody
    ): Call<ResponseBody>
}
