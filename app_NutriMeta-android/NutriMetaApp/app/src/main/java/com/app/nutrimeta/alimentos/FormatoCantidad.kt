package com.app.nutrimeta.alimentos

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

// Cantidades sin ceros sobrantes y siempre con punto decimal:
// 150.0 -> "150", 37.5 -> "37.5". Se usa solo desde el hilo principal.
object FormatoCantidad {

    private val formato = DecimalFormat("0.##", DecimalFormatSymbols(Locale.US))

    fun texto(valor: Double): String {
        return formato.format(valor)
    }
}
