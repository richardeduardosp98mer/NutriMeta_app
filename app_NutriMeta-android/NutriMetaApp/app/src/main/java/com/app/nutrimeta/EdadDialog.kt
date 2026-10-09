
package com.app.nutrimeta

import android.app.DatePickerDialog
import android.view.View
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import java.util.Calendar
import java.util.Locale

class EdadDialog(
    private val fechaPrevia: String? = null,
    private val onFechaGuardada: (String, Int) -> Unit
) : PadreDialog() {

    override val tituloResId: Int =
        R.string.titulo_dialogo_edad

    override val layoutContenidoResId: Int =
        R.layout.activity_edad_dialog

    private var fechaSeleccionada: Calendar? = null

    override fun configurarContenido(viewHijo: View) {

        val tvValor = viewHijo.findViewById<TextView>(
            R.id.tvValorEdadDialog
        )

        val btnGuardar = viewHijo.findViewById<MaterialButton>(
            R.id.btnGuardarEdad
        )

        fechaSeleccionada = convertirFecha(fechaPrevia)

        fechaSeleccionada?.let {
            tvValor.text = formatearFecha(it)
        }

        btnGuardar.isEnabled = fechaSeleccionada != null

        // Abrir calendario
        tvValor.setOnClickListener {

            // Solo usuarios entre 18 y 100 años.
            val fechaMaxima = Calendar.getInstance().apply {
                add(Calendar.YEAR, -18)
            }

            val fechaMinima = Calendar.getInstance().apply {
                add(Calendar.YEAR, -101)
                add(Calendar.DAY_OF_MONTH, 1)
            }

            val inicial = fechaSeleccionada
                ?: Calendar.getInstance().apply {
                    add(Calendar.YEAR, -25)
                }

            val calendario = DatePickerDialog(
                requireContext(),
                { _, anio, mes, dia ->

                    fechaSeleccionada =
                        Calendar.getInstance().apply {
                            clear()
                            set(anio, mes, dia)
                        }

                    val fecha = fechaSeleccionada!!

                    tvValor.text = formatearFecha(fecha)

                    btnGuardar.isEnabled = true

                },
                inicial.get(Calendar.YEAR),
                inicial.get(Calendar.MONTH),
                inicial.get(Calendar.DAY_OF_MONTH)
            )

            calendario.datePicker.minDate =
                fechaMinima.timeInMillis

            calendario.datePicker.maxDate =
                fechaMaxima.timeInMillis

            calendario.show()
        }

        // Guardar fecha y edad
        btnGuardar.setOnClickListener {

            val fecha = fechaSeleccionada
                ?: return@setOnClickListener

            val fechaISO = String.format(
                Locale.US,
                "%04d-%02d-%02d",
                fecha.get(Calendar.YEAR),
                fecha.get(Calendar.MONTH) + 1,
                fecha.get(Calendar.DAY_OF_MONTH)
            )

            val edad = calcularEdad(fecha)

            onFechaGuardada(fechaISO, edad)

            dismiss()
        }
    }

    private fun formatearFecha(
        fecha: Calendar
    ): String {

        return getString(
            R.string.formato_fecha_nacimiento,
            fecha.get(Calendar.DAY_OF_MONTH),
            fecha.get(Calendar.MONTH) + 1,
            fecha.get(Calendar.YEAR)
        )
    }

    private fun calcularEdad(
        nacimiento: Calendar
    ): Int {

        val hoy = Calendar.getInstance()

        var edad = hoy.get(Calendar.YEAR) -
                nacimiento.get(Calendar.YEAR)

        val mesActual = hoy.get(Calendar.MONTH)
        val mesNacimiento = nacimiento.get(Calendar.MONTH)

        val diaActual = hoy.get(Calendar.DAY_OF_MONTH)
        val diaNacimiento = nacimiento.get(Calendar.DAY_OF_MONTH)

        if (
            mesActual < mesNacimiento ||
            (mesActual == mesNacimiento &&
                    diaActual < diaNacimiento)
        ) {
            edad--
        }

        return edad
    }

    private fun convertirFecha(
        valor: String?
    ): Calendar? {

        if (valor.isNullOrBlank()) return null

        return runCatching {

            val partes = valor.split("-")
            require(partes.size == 3)

            Calendar.getInstance().apply {
                clear()
                isLenient = false

                set(
                    partes[0].toInt(),
                    partes[1].toInt() - 1,
                    partes[2].toInt()
                )

                timeInMillis
            }

        }.getOrNull()
    }
}
