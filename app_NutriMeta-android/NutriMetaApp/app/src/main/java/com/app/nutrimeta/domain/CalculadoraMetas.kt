
package com.app.nutrimeta.domain

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

object CalculadoraMetas {

    fun calcular(
        gastoDiarioKcal: Double,
        pesoKg: Double,
        objetivo: ObjetivoNutricional
    ): MetasNutricionales {

        require(
            gastoDiarioKcal.isFinite() &&
                    gastoDiarioKcal > 0.0
        ) {
            "El gasto energético debe ser válido"
        }

        require(
            pesoKg.isFinite() && pesoKg > 0.0
        ) {
            "El peso debe ser válido"
        }

        // Ajuste de calorías según el objetivo.
        val factorObjetivo = when (objetivo) {
            ObjetivoNutricional.BAJAR -> 0.90
            ObjetivoNutricional.MANTENER -> 1.0
            ObjetivoNutricional.SUBIR -> 1.05
        }

        val metaKcal = gastoDiarioKcal * factorObjetivo

        // Proteína en gramos por kilogramo.
        val factorProteina = when (objetivo) {
            ObjetivoNutricional.BAJAR -> 1.8
            ObjetivoNutricional.MANTENER -> 1.6
            ObjetivoNutricional.SUBIR -> 1.8
        }

        val proteinaG = pesoKg * factorProteina

        // Grasas: 25% de las calorías.
        val grasaKcal = metaKcal * 0.25
        val grasaG = grasaKcal / 9.0

        // Carbohidratos: calorías restantes.
        val proteinaKcal = proteinaG * 4.0

        val carbohidratosKcal =
            metaKcal - proteinaKcal - grasaKcal

        require(carbohidratosKcal >= 0.0) {
            "No es posible calcular macros coherentes con estos datos"
        }

        val carbohidratosG = carbohidratosKcal / 4.0

        return MetasNutricionales(
            metaKcal = metaKcal,
            metaProteinaG = proteinaG,
            metaGrasaG = grasaG,
            metaCarbohidratosG = carbohidratosG
        )
    }
}
