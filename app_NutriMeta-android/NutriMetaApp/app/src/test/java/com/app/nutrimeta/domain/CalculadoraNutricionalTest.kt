
package com.app.nutrimeta.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CalculadoraNutricionalTest {

    @Test
    fun calcularMetabolismoHombre() {

        val datos = DatosCalculo(
            sexo = SexoBiologico.M,
            edad = 20,
            pesoKg = 70.0,
            tallaCm = 170.0,
            factorActividad = 1.0
        )

        val resultado = CalculadoraNutricional.calcular(datos)

        assertEquals(
            1667.5,
            resultado.metabolismoBasalKcal,
            0.01
        )

        assertEquals(
            1667.5,
            resultado.gastoDiarioKcal,
            0.01
        )
    }

    @Test
    fun calcularMetabolismoMujer() {

        val datos = DatosCalculo(
            sexo = SexoBiologico.F,
            edad = 25,
            pesoKg = 60.0,
            tallaCm = 165.0,
            factorActividad = 1.0
        )

        val resultado = CalculadoraNutricional.calcular(datos)

        assertEquals(
            1345.25,
            resultado.metabolismoBasalKcal,
            0.01
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rechazarEdadMenorDe18() {

        val datos = DatosCalculo(
            sexo = SexoBiologico.M,
            edad = 16,
            pesoKg = 60.0,
            tallaCm = 170.0,
            factorActividad = 1.0
        )

        CalculadoraNutricional.calcular(datos)
    }
}
