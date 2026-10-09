
package com.app.nutrimeta.domain

object CalculadoraNutricional {

    fun calcular(
        datos: DatosCalculo
    ): ResultadoEnergetico {

        // Validaciones de entrada
        require(datos.edad in 18..100) {
            "La edad debe estar entre 18 y 100 años"
        }

        require(
            datos.pesoKg.isFinite() &&
                    datos.pesoKg > 0.0
        ) {
            "El peso debe ser válido"
        }

        require(
            datos.tallaCm.isFinite() &&
                    datos.tallaCm > 0.0
        ) {
            "La altura debe ser válida"
        }

        require(
            datos.factorActividad.isFinite() &&
                    datos.factorActividad > 0.0
        ) {
            "El factor de actividad debe ser válido"
        }

        // Fórmula Mifflin-St Jeor
        val ajusteSexo = when (datos.sexo) {
            SexoBiologico.M -> 5.0
            SexoBiologico.F -> -161.0
        }

        val metabolismoBasal =
            (10.0 * datos.pesoKg) +
                    (6.25 * datos.tallaCm) -
                    (5.0 * datos.edad) +
                    ajusteSexo

        // Gasto energético total diario
        val gastoDiario =
            metabolismoBasal * datos.factorActividad

        return ResultadoEnergetico(
            metabolismoBasalKcal = metabolismoBasal,
            gastoDiarioKcal = gastoDiario
        )
    }
}
