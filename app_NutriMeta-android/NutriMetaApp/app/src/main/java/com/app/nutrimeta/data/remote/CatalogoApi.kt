package com.app.nutrimeta.data.remote

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface CatalogoApi {

    // desde: fecha ISO de la ultima sincronizacion (null = catalogo completo).
    // Se recibe el texto del JSON y lo interpreta CatalogoJson.
    @GET("api/catalogo")
    fun obtener(@Query("desde") desde: String?): Call<ResponseBody>
}
