package com.app.nutrimeta.ui.dialogs

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import com.app.nutrimeta.R

abstract class PadreDialog : DialogFragment() {

    // Cada hijo solo definirá su título y su layout interno
    @get:StringRes
    abstract val tituloResId: Int

    @get:LayoutRes
    abstract val layoutContenidoResId: Int

    // Cada hijo programará sus propios botones aquí adentro
    abstract fun configurarContenido(viewHijo: View)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.activity_padre_dialog, container, false)

        val tvTitulo = root.findViewById<TextView>(R.id.tvTituloBaseDialog)
        val btnCerrar = root.findViewById<ImageView>(R.id.btnCerrarBaseDialog)
        val contenedor = root.findViewById<FrameLayout>(R.id.contenedorContenidoDialog)

        // Asigna el título y la acción de cerrar universal
        tvTitulo.setText(tituloResId)
        btnCerrar.setOnClickListener { dismiss() }

        // Infla e inyecta la vista del hijo
        val viewHijo = inflater.inflate(layoutContenidoResId, contenedor, false)
        contenedor.addView(viewHijo)

        // Llama a la configuración del hijo
        configurarContenido(viewHijo)

        return root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }
}