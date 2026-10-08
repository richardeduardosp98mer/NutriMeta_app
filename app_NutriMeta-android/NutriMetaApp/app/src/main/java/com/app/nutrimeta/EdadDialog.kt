package com.app.nutrimeta

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.button.MaterialButton

class EdadDialog(
    private val edadPrevia: Int? = null,
    private val onEdadGuardada: (Int) -> Unit
) : PadreDialog() {

    override val tituloResId: Int = R.string.titulo_dialogo_edad
    override val layoutContenidoResId: Int = R.layout.activity_edad_dialog

    private var edadActual: Int = edadPrevia ?: 25

    override fun configurarContenido(viewHijo: View) {
        val tvValor = viewHijo.findViewById<TextView>(R.id.tvValorEdadDialog)
        val btnRestar = viewHijo.findViewById<ImageView>(R.id.btnRestarEdad)
        val btnSumar = viewHijo.findViewById<ImageView>(R.id.btnSumarEdad)
        val btnGuardar = viewHijo.findViewById<MaterialButton>(R.id.btnGuardarEdad)

        tvValor.text = edadActual.toString()

        btnRestar.setOnClickListener {
            if (edadActual >= 19) {
                edadActual--
                tvValor.text = edadActual.toString()
            }
        }

        btnSumar.setOnClickListener {
            if (edadActual < 100) {
                edadActual++
                tvValor.text = edadActual.toString()
            }
        }

        btnGuardar.setOnClickListener {
            onEdadGuardada(edadActual)
            dismiss()
        }
    }
}