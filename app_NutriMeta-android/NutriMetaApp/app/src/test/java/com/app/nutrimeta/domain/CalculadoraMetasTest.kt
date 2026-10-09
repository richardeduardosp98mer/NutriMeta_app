
package com.app.nutrimeta.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CalculadoraMetasTest {

    @Test
    fun calcularMetaPerderGrasa() {

        val resultado = CalculadoraMetas.calcular(
            gastoDiarioKcal = 2400.0,
            pesoKg = 70.0,
            objetivo = ObjetivoNutricional.BAJAR
        )

        assertEquals(2160.0, resultado.metaKcal, 0.001)
        assertEquals(126.0, resultado.metaProteinaG, 0.001)
        assertEquals(60.0, resultado.metaGrasaG, 0.001)
        assertEquals(279.0, resultado.metaCarbohidratosG, 0.001)
    }

    @Test
    fun calcularMetaMantenerPeso() {

        val resultado = CalculadoraMetas.calcular(
            gastoDiarioKcal = 2400.0,
            pesoKg = 70.0,
            objetivo = ObjetivoNutricional.MANTENER
        )

        assertEquals(2400.0, resultado.metaKcal, 0.001)
        assertEquals(112.0, resultado.metaProteinaG, 0.001)
    }

    @Test
    fun calcularMetaGanarMusculo() {

        val resultado = CalculadoraMetas.calcular(
            gastoDiarioKcal = 2400.0,
            pesoKg = 70.0,
            objetivo = ObjetivoNutricional.SUBIR
        )

        assertEquals(2520.0, resultado.metaKcal, 0.001)
        assertEquals(126.0, resultado.metaProteinaG, 0.001)
    }

    @Test
    fun caloriasDeMacrosCoincidenConLaMeta() {

        val resultado = CalculadoraMetas.calcular(
            gastoDiarioKcal = 2400.0,
            pesoKg = 70.0,
            objetivo = ObjetivoNutricional.BAJAR
        )

        val caloriasMacros =
            resultado.metaProteinaG * 4.0 +
                    resultado.metaGrasaG * 9.0 +
                    resultado.metaCarbohidratosG * 4.0

        assertEquals(
            resultado.metaKcal,
            caloriasMacros,
            0.001
        )
    }
}
