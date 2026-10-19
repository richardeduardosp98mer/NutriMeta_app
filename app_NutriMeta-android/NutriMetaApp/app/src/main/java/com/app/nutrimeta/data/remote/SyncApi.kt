package com.app.nutrimeta.data.remote

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface SyncApi {

    // Sube el perfil y los registros de comida pendientes.
    // autorizacion: "Bearer <ID token de Firebase>".
    // El cuerpo y la respuesta son JSON armados y leidos a mano.
    @POST("api/sync/subir")
    fun subir(
        @Header("Authorization") autorizacion: String,
        @Body cuerpo: RequestBody
    ): Call<ResponseBody>
}
