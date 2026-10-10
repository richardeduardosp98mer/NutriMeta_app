package com.app.nutrimeta.ui.dialogs

import android.view.View
import android.widget.ImageView
import com.app.nutrimeta.R
import com.google.android.material.card.MaterialCardView

class SexoDialog(
    private val seleccionPrevia: String? = null,
    private val onSexoSeleccionado: (String) -> Unit
) : PadreDialog() {

    override val tituloResId: Int = R.string.titulo_dialogo_sexo
    override val layoutContenidoResId: Int = R.layout.activity_sexo_dialog

    override fun configurarContenido(viewHijo: View) {
        val cardHombre = viewHijo.findViewById<MaterialCardView>(R.id.cardOpcionHombre)
        val cardMujer = viewHijo.findViewById<MaterialCardView>(R.id.cardOpcionMujer)
        val ivRadioHombre = viewHijo.findViewById<ImageView>(R.id.ivRadioHombre)
        val ivRadioMujer = viewHijo.findViewById<ImageView>(R.id.ivRadioMujer)

        val textoHombre = getString(R.string.opcion_sexo_hombre)
        val textoMujer = getString(R.string.opcion_sexo_mujer)

        // Marcar la selección anterior si ya existía
        if (seleccionPrevia == textoHombre) {
            ivRadioHombre.setImageResource(R.drawable.ic_radio_activo)
        } else if (seleccionPrevia == textoMujer) {
            ivRadioMujer.setImageResource(R.drawable.ic_radio_activo)
        }

        // Selección Hombre
        cardHombre.setOnClickListener {
            ivRadioHombre.setImageResource(R.drawable.ic_radio_activo)
            ivRadioMujer.setImageResource(R.drawable.ic_radio_inactivo)
            onSexoSeleccionado(textoHombre)
            dismiss()
        }

        // Selección Mujer
        cardMujer.setOnClickListener {
            ivRadioMujer.setImageResource(R.drawable.ic_radio_activo)
            ivRadioHombre.setImageResource(R.drawable.ic_radio_inactivo)
            onSexoSeleccionado(textoMujer)
            dismiss()
        }
    }
}