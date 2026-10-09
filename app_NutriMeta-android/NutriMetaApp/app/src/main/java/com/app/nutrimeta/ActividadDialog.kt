package com.app.nutrimeta

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.RadioGroup
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton

class ActividadDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val ventana = Dialog(requireContext())
        val vista = LayoutInflater.from(requireContext())
            .inflate(R.layout.activity_actividad_dialog, null)

        val grupo = vista.findViewById<RadioGroup>(R.id.grupoActividad)
        val btnGuardar = vista.findViewById<MaterialButton>(R.id.btnGuardarActividad)

        val opciones = mapOf(
            R.id.radioSedentario to SEDENTARIO,
            R.id.radioLigero to LIGERO,
            R.id.radioModerado to MODERADO,
            R.id.radioMuyActivo to MUY_ACTIVO,
            R.id.radioExtremo to EXTREMO
        )

        val anterior = arguments?.getString(ARG_ACTIVIDAD)
        opciones.entries.firstOrNull { it.value == anterior }?.let {
            grupo.check(it.key)
        }

        btnGuardar.isEnabled = grupo.checkedRadioButtonId != -1
        grupo.setOnCheckedChangeListener { _, radioSeleccionado ->
            btnGuardar.isEnabled = radioSeleccionado != -1
        }

        btnGuardar.setOnClickListener {
            val codigo = opciones[grupo.checkedRadioButtonId]
                ?: return@setOnClickListener

            val resultado = Bundle().apply {
                putString(RESULT_KEY, codigo)
            }
            parentFragmentManager.setFragmentResult(REQUEST_KEY, resultado)
            dismiss()
        }

        ventana.setContentView(vista)
        ventana.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return ventana
    }

    override fun onStart() {
        super.onStart()
        // Ajustar el ancho: como máximo 340dp, con márgenes en pantallas estrechas.
        val margen = resources.getDimensionPixelSize(R.dimen.dp_16)
        val maxAncho = resources.getDimensionPixelSize(R.dimen.dp_340)
        val ancho = minOf(resources.displayMetrics.widthPixels - margen * 2, maxAncho)
        dialog?.window?.setLayout(ancho, WindowManager.LayoutParams.WRAP_CONTENT)
    }

    companion object {
        const val REQUEST_KEY = "resultado_dialogo_actividad"
        const val RESULT_KEY = "nivel_actividad"

        const val SEDENTARIO = "SEDENTARIO"
        const val LIGERO = "LIGERO"
        const val MODERADO = "MODERADO"
        const val MUY_ACTIVO = "MUY_ACTIVO"
        const val EXTREMO = "EXTREMO"

        private const val ARG_ACTIVIDAD = "actividad_previa"

        fun nuevaInstancia(nivelAnterior: String?): ActividadDialog {
            return ActividadDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_ACTIVIDAD, nivelAnterior)
                }
            }
        }
    }
}
