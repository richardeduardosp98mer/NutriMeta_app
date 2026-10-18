package com.app.nutrimeta.data.local

import org.json.JSONObject

data class GrupoCatalogo(
    val idGrupo: Int,
    val codigo: String,
    val nombre: String
)

data class AlimentoCatalogo(
    val idAlimento: Long,
    val codigoIns: String?,
    val idGrupo: Int,
    val nombre: String,
    val unidad: String,
    val energiaKcal: Double,
    val proteinaG: Double,
    val grasaG: Double,
    val carbohidratosG: Double,
    val fibraG: Double?,
    val metodoCarbohidratos: String,
    val fuente: String,
    val activo: Boolean
)

data class CatalogoRespuesta(
    val sincronizadoEn: String?,
    val grupos: List<GrupoCatalogo>,
    val alimentos: List<AlimentoCatalogo>
)

// Lee el JSON de GET /api/catalogo. El seed de assets usa exactamente el
// mismo formato, asi que un solo parser sirve para el seed y para la red.
object CatalogoJson {

    fun parsear(texto: String): CatalogoRespuesta {

        val raiz = JSONObject(texto)

        val grupos = ArrayList<GrupoCatalogo>()
        val arregloGrupos = raiz.optJSONArray("grupos")

        if (arregloGrupos != null) {
            for (i in 0 until arregloGrupos.length()) {
                val g = arregloGrupos.getJSONObject(i)

                grupos.add(
                    GrupoCatalogo(
                        idGrupo = g.getInt("idGrupo"),
                        codigo = g.getString("codigo"),
                        nombre = g.getString("nombre")
                    )
                )
            }
        }

        val alimentos = ArrayList<AlimentoCatalogo>()
        val arregloAlimentos = raiz.optJSONArray("alimentos")

        if (arregloAlimentos != null) {
            for (i in 0 until arregloAlimentos.length()) {
                val a = arregloAlimentos.getJSONObject(i)

                alimentos.add(
                    AlimentoCatalogo(
                        idAlimento = a.getLong("idAlimento"),
                        codigoIns = textoONulo(a, "codigoIns"),
                        idGrupo = a.getInt("idGrupo"),
                        nombre = a.getString("nombre"),
                        unidad = a.getString("unidad"),
                        energiaKcal = a.getDouble("energiaKcal"),
                        proteinaG = a.getDouble("proteinaG"),
                        grasaG = a.getDouble("grasaG"),
                        carbohidratosG = a.getDouble("carbohidratosG"),
                        // fibraG null significa "sin dato", no cero.
                        fibraG = if (a.isNull("fibraG")) null else a.getDouble("fibraG"),
                        metodoCarbohidratos = a.getString("metodoCarbohidratos"),
                        fuente = a.getString("fuente"),
                        activo = a.getBoolean("activo")
                    )
                )
            }
        }

        return CatalogoRespuesta(
            sincronizadoEn = textoONulo(raiz, "sincronizadoEn"),
            grupos = grupos,
            alimentos = alimentos
        )
    }

    // JSONObject.optString devolveria el texto "null"; por eso se pregunta antes.
    private fun textoONulo(objeto: JSONObject, clave: String): String? {
        return if (objeto.isNull(clave)) null else objeto.getString(clave)
    }
}
