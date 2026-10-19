
package com.app.nutrimeta.domain

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.min

enum class ObjetivoNutricional {
    BAJAR,
    MANTENER,
    SUBIR
}

data class MetasNutricionales(
    val metaKcal: Double,
    val metaProteinaG: Double,
    val metaGrasaG: Double,
    val metaCarbohidratosG: Double
)

// Resultado de calcular la meta teniendo en cuenta el peso objetivo y el plazo.
data class ResultadoMeta(
    val metas: MetasNutricionales,
    // true si la meta pedida no era segura y se ajusto al maximo seguro.
    val ajustada: Boolean,
    // kcal por dia de deficit (bajar) o superavit (subir) que pedia el usuario.
    val cambioSolicitadoKcal: Double,
    // kcal por dia que realmente se aplican.
    val cambioAplicadoKcal: Double,
    // Semanas que tardaria llegar al peso objetivo con el cambio aplicado
    // (null si no hay peso objetivo o no hay cambio).
    val semanasReales: Int?
)

// Limites usados para validar la meta del usuario. Valores de partida a partir de
// recomendaciones habituales; el equipo debe confirmarlos con fuentes citables.
object LimitesSeguros {
    // Energia equivalente a 1 kg de peso corporal.
    const val KCAL_POR_KG = 7700.0

    // Bajar: deficit diario maximo (kcal), el menor entre un tope fijo, una fraccion
    // del gasto diario y un ritmo de 1 % del peso corporal por semana.
    const val DEFICIT_MAXIMO_KCAL = 1000.0
    const val DEFICIT_MAXIMO_FRACCION_GASTO = 0.25
    const val RITMO_MAXIMO_BAJAR_FRACCION_PESO_SEMANA = 0.01

    // Subir: superavit diario maximo (kcal).
    const val SUPERAVIT_MAXIMO_KCAL = 500.0

    // Al bajar, la meta diaria nunca baja de este piso (kcal).
    const val PISO_KCAL_MUJER = 1200.0
    const val PISO_KCAL_HOMBRE = 1500.0

    const val PLAZO_MAXIMO_SEMANAS = 104
}

object CalculadoraMetas {

    // Meta sin peso objetivo ni plazo: solo el objetivo (bajar, mantener o subir)
    // ajusta las calorias con un porcentaje fijo.
    fun calcular(
        gastoDiarioKcal: Double,
        pesoKg: Double,
        objetivo: ObjetivoNutricional
    ): MetasNutricionales {

        validarBase(gastoDiarioKcal, pesoKg)

        // Ajuste de calorias segun el objetivo.
        val factorObjetivo = when (objetivo) {
            ObjetivoNutricional.BAJAR -> 0.90
            ObjetivoNutricional.MANTENER -> 1.0
            ObjetivoNutricional.SUBIR -> 1.05
        }

        return macros(gastoDiarioKcal * factorObjetivo, pesoKg, objetivo)
    }

    // Meta con peso objetivo y plazo. Si el cambio de calorias que exige la meta
    // supera los limites seguros, se ajusta al maximo seguro y se indica en
    // `ajustada` (y en `semanasReales` cuanto tardaria realmente).
    // Sin peso objetivo o plazo, o con objetivo MANTENER, equivale a `calcular`.
    fun calcularConMeta(
        gastoDiarioKcal: Double,
        pesoKg: Double,
        objetivo: ObjetivoNutricional,
        sexo: SexoBiologico,
        pesoObjetivoKg: Double?,
        plazoSemanas: Int?
    ): ResultadoMeta {

        validarBase(gastoDiarioKcal, pesoKg)

        if (objetivo == ObjetivoNutricional.MANTENER ||
            pesoObjetivoKg == null ||
            plazoSemanas == null
        ) {
            return ResultadoMeta(
                metas = calcular(gastoDiarioKcal, pesoKg, objetivo),
                ajustada = false,
                cambioSolicitadoKcal = 0.0,
                cambioAplicadoKcal = 0.0,
                semanasReales = null
            )
        }

        require(pesoObjetivoKg.isFinite() && pesoObjetivoKg > 0.0) {
            "El peso objetivo debe ser valido"
        }

        require(plazoSemanas in 1..LimitesSeguros.PLAZO_MAXIMO_SEMANAS) {
            "El plazo debe estar entre 1 y ${LimitesSeguros.PLAZO_MAXIMO_SEMANAS} semanas"
        }

        when (objetivo) {
            ObjetivoNutricional.BAJAR -> require(pesoObjetivoKg < pesoKg) {
                "Para bajar, el peso objetivo debe ser menor al peso actual"
            }

            ObjetivoNutricional.SUBIR -> require(pesoObjetivoKg > pesoKg) {
                "Para subir, el peso objetivo debe ser mayor al peso actual"
            }

            ObjetivoNutricional.MANTENER -> Unit
        }

        val kcalTotales = abs(pesoObjetivoKg - pesoKg) * LimitesSeguros.KCAL_POR_KG
        val solicitado = kcalTotales / (plazoSemanas * 7.0)

        val maximo = when (objetivo) {
            ObjetivoNutricional.BAJAR -> min(
                LimitesSeguros.DEFICIT_MAXIMO_KCAL,
                min(
                    LimitesSeguros.DEFICIT_MAXIMO_FRACCION_GASTO * gastoDiarioKcal,
                    pesoKg * LimitesSeguros.RITMO_MAXIMO_BAJAR_FRACCION_PESO_SEMANA *
                            LimitesSeguros.KCAL_POR_KG / 7.0
                )
            )

            ObjetivoNutricional.SUBIR -> LimitesSeguros.SUPERAVIT_MAXIMO_KCAL
            ObjetivoNutricional.MANTENER -> 0.0
        }

        var aplicado = min(solicitado, maximo)

        if (objetivo == ObjetivoNutricional.BAJAR) {
            val piso = when (sexo) {
                SexoBiologico.F -> LimitesSeguros.PISO_KCAL_MUJER
                SexoBiologico.M -> LimitesSeguros.PISO_KCAL_HOMBRE
            }
            aplicado = min(aplicado, (gastoDiarioKcal - piso).coerceAtLeast(0.0))
        }

        val ajustada = solicitado - aplicado > 1.0

        val metaKcal = if (objetivo == ObjetivoNutricional.BAJAR) {
            gastoDiarioKcal - aplicado
        } else {
            gastoDiarioKcal + aplicado
        }

        val semanas = if (aplicado > 0.0) {
            ceil(kcalTotales / (aplicado * 7.0)).toInt()
        } else {
            null
        }

        return ResultadoMeta(
            metas = macros(metaKcal, pesoKg, objetivo),
            ajustada = ajustada,
            cambioSolicitadoKcal = solicitado,
            cambioAplicadoKcal = aplicado,
            semanasReales = semanas
        )
    }

    private fun validarBase(gastoDiarioKcal: Double, pesoKg: Double) {

        require(
            gastoDiarioKcal.isFinite() &&
                    gastoDiarioKcal > 0.0
        ) {
            "El gasto energetico debe ser valido"
        }

        require(
            pesoKg.isFinite() && pesoKg > 0.0
        ) {
            "El peso debe ser valido"
        }
    }

    // Reparte la meta calorica en macros: proteina por kg de peso, grasa 25 % de las
    // calorias y carbohidratos con el resto.
    private fun macros(
        metaKcal: Double,
        pesoKg: Double,
        objetivo: ObjetivoNutricional
    ): MetasNutricionales {

        val factorProteina = when (objetivo) {
            ObjetivoNutricional.BAJAR -> 1.8
            ObjetivoNutricional.MANTENER -> 1.6
            ObjetivoNutricional.SUBIR -> 1.8
        }

        val grasaKcal = metaKcal * 0.25
        val grasaG = grasaKcal / 9.0

        // Si la proteina por kg no cabe en la meta (personas muy pesadas con meta baja),
        // se limita a lo que queda tras la grasa en vez de fallar.
        val proteinaKcal = min(pesoKg * factorProteina * 4.0, metaKcal - grasaKcal)
            .coerceAtLeast(0.0)

        val proteinaG = proteinaKcal / 4.0

        val carbohidratosKcal = metaKcal - proteinaKcal - grasaKcal

        require(carbohidratosKcal >= 0.0) {
            "No es posible calcular macros coherentes con estos datos"
        }

        return MetasNutricionales(
            metaKcal = metaKcal,
            metaProteinaG = proteinaG,
            metaGrasaG = grasaG,
            metaCarbohidratosG = carbohidratosKcal / 4.0
        )
    }
}
