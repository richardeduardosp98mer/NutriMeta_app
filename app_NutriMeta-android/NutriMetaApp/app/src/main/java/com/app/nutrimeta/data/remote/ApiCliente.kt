package com.app.nutrimeta.data.remote

import com.app.nutrimeta.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

// Cliente HTTP compartido. La URL base viene de local.properties (api.baseUrl).
object ApiCliente {

    private val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        val base = BuildConfig.API_BASE_URL.trim()

        Retrofit.Builder()
            .baseUrl(if (base.endsWith("/")) base else "$base/")
            .client(http)
            .build()
    }

    val catalogo: CatalogoApi by lazy {
        retrofit.create(CatalogoApi::class.java)
    }

    val usuarios: UsuarioApi by lazy {
        retrofit.create(UsuarioApi::class.java)
    }
}
