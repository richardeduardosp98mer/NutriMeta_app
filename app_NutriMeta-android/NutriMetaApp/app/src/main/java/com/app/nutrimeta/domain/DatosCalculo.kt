
package com.app.nutrimeta.domain

enum class SexoBiologico {
    M,
    F
}

data class DatosCalculo(
    val sexo: SexoBiologico,
    val edad: Int,
    val pesoKg: Double,
    val tallaCm: Double,
    val factorActividad: Double
)
