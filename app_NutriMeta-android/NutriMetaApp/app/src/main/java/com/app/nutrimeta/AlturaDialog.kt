package com.app.nutrimeta

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.button.MaterialButton

class AlturaDialog(
    private val alturaPrevia: Int? = null,
    private val onAlturaGuardada: (Int) -> Unit
) : PadreDialog() {

    override val tituloResId: Int = R.string.titulo_dialogo_altura
    override val layoutContenidoResId: Int = R.layout.activity_altura_dialog

    private var alturaActual: Int = alturaPrevia ?: 170

    override fun configurarContenido(viewHijo: View) {
        val tvValor = viewHijo.findViewById<TextView>(R.id.tvValorAlturaDialog)
        val btnRestar = viewHijo.findViewById<ImageView>(R.id.btnRestarAltura)
        val btnSumar = viewHijo.findViewById<ImageView>(R.id.btnSumarAltura)
        val btnGuardar = viewHijo.findViewById<MaterialButton>(R.id.btnGuardarAltura)

        tvValor.text = alturaActual.toString()

        btnRestar.setOnClickListener {
            if (alturaActual > 100) {
                alturaActual--
                tvValor.text = alturaActual.toString()
            }
        }

        btnSumar.setOnClickListener {
            if (alturaActual < 250) {
                alturaActual++
                tvValor.text = alturaActual.toString()
            }
        }

        btnGuardar.setOnClickListener {
            onAlturaGuardada(alturaActual)
            dismiss()
        }
    }
}