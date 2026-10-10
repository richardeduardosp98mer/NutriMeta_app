package com.app.nutrimeta.onboarding

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.app.nutrimeta.ui.dialogs.ActividadDialog
import com.app.nutrimeta.ui.dialogs.AlturaDialog
import com.app.nutrimeta.ui.dialogs.EdadDialog
import com.app.nutrimeta.ui.dialogs.PesoDialog
import com.app.nutrimeta.R
import com.app.nutrimeta.ui.dialogs.SexoDialog
import com.app.nutrimeta.auth.RegistroActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class SobreUsuarioActivity : AppCompatActivity() {

    private var sexoSeleccionado: String? = null
    private var edadSeleccionada: Int? = null
    private var fechaNacimientoSeleccionada: String? = null

    private var alturaSeleccionada: Int? = null
    private var pesoSeleccionado: Double? = null
    private var actividadSeleccionada: String? = null

    private lateinit var btnContinuar: MaterialButton

    private lateinit var tvValorSexo: TextView
    private lateinit var tvValorEdad: TextView
    private lateinit var tvValorAltura: TextView
    private lateinit var tvValorPeso: TextView
    private lateinit var tvValorActividad: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_sobre_usuario)

        // Objetivo recibido desde la pantalla anterior
        val objetivo = intent.getStringExtra(
            "EXTRA_OBJETIVO"
        ) ?: "PERDER_GRASA"

        // Referencias a las vistas
        btnContinuar = findViewById(R.id.btnContinuarSobreTi)

        tvValorSexo = findViewById(R.id.tvValorSexo)
        tvValorEdad = findViewById(R.id.tvValorEdad)
        tvValorAltura = findViewById(R.id.tvValorAltura)
        tvValorPeso = findViewById(R.id.tvValorPeso)
        tvValorActividad = findViewById(R.id.tvValorActividad)

        // RESTAURAR VALORES SI ANDROID RECREA LA ACTIVITY
        if (savedInstanceState != null) {

            sexoSeleccionado = savedInstanceState.getString(
                KEY_SEXO
            )

            if (savedInstanceState.containsKey(KEY_EDAD)) {
                edadSeleccionada = savedInstanceState.getInt(
                    KEY_EDAD
                )
            }

            // NUEVO: restaurar fecha de nacimiento
            fechaNacimientoSeleccionada =
                savedInstanceState.getString(
                    KEY_FECHA_NACIMIENTO
                )

            if (savedInstanceState.containsKey(KEY_ALTURA)) {
                alturaSeleccionada = savedInstanceState.getInt(
                    KEY_ALTURA
                )
            }

            if (savedInstanceState.containsKey(KEY_PESO)) {
                pesoSeleccionado = savedInstanceState.getDouble(
                    KEY_PESO
                )
            }

            actividadSeleccionada = savedInstanceState.getString(
                KEY_ACTIVIDAD
            )
        }

        // RESULTADO DEL DIÁLOGO DE ACTIVIDAD FÍSICA
        supportFragmentManager.setFragmentResultListener(
            ActividadDialog.Companion.REQUEST_KEY,
            this
        ) { _, resultado ->

            actividadSeleccionada = resultado.getString(
                ActividadDialog.Companion.RESULT_KEY
            )

            actualizarValores()
            validarFormularioCompleto()
        }

        // BOTÓN VOLVER
        findViewById<ImageView>(
            R.id.btnVolverSobreTi
        ).setOnClickListener {
            finish()
        }

        // SELECCIONAR SEXO
        findViewById<MaterialCardView>(
            R.id.cardSexo
        ).setOnClickListener {

            SexoDialog(sexoSeleccionado) { seleccion ->

                sexoSeleccionado = seleccion

                actualizarValores()
                validarFormularioCompleto()

            }.show(
                supportFragmentManager,
                "SexoDialog"
            )
        }

        // SELECCIONAR FECHA DE NACIMIENTO
        findViewById<MaterialCardView>(
            R.id.cardEdad
        ).setOnClickListener {

            EdadDialog(
                fechaPrevia = fechaNacimientoSeleccionada
            ) { fechaNacimiento, edad ->

                // Guardamos la fecha completa
                fechaNacimientoSeleccionada = fechaNacimiento

                // Guardamos la edad calculada
                edadSeleccionada = edad

                actualizarValores()
                validarFormularioCompleto()

            }.show(
                supportFragmentManager,
                "EdadDialog"
            )
        }

        // SELECCIONAR ALTURA
        findViewById<MaterialCardView>(
            R.id.cardAltura
        ).setOnClickListener {

            AlturaDialog(
                alturaPrevia = alturaSeleccionada
            ) { altura ->

                alturaSeleccionada = altura

                actualizarValores()
                validarFormularioCompleto()

            }.show(
                supportFragmentManager,
                "AlturaDialog"
            )
        }

        // SELECCIONAR PESO
        findViewById<MaterialCardView>(
            R.id.cardPeso
        ).setOnClickListener {

            PesoDialog(
                pesoPrevio = pesoSeleccionado
            ) { peso ->

                pesoSeleccionado = peso

                actualizarValores()
                validarFormularioCompleto()

            }.show(
                supportFragmentManager,
                "PesoDialog"
            )
        }

        // SELECCIONAR ACTIVIDAD FÍSICA
        findViewById<MaterialCardView>(
            R.id.cardActividad
        ).setOnClickListener {

            ActividadDialog.Companion.nuevaInstancia(
                actividadSeleccionada
            ).show(
                supportFragmentManager,
                "ActividadDialog"
            )
        }

        // CONTINUAR AL REGISTRO
        btnContinuar.setOnClickListener {

            if (!formularioCompleto()) {
                return@setOnClickListener
            }

            val siguientePantalla = Intent(
                this,
                RegistroActivity::class.java
            ).apply {

                putExtra(
                    "EXTRA_OBJETIVO",
                    objetivo
                )

                putExtra(
                    "EXTRA_SEXO",
                    sexoSeleccionado
                )

                putExtra(
                    "EXTRA_EDAD",
                    edadSeleccionada ?: 0
                )

                // NUEVO: enviar fecha de nacimiento
                putExtra(
                    "EXTRA_FECHA_NACIMIENTO",
                    fechaNacimientoSeleccionada
                )

                putExtra(
                    "EXTRA_ALTURA",
                    alturaSeleccionada ?: 0
                )

                putExtra(
                    "EXTRA_PESO",
                    pesoSeleccionado ?: 0.0
                )

                putExtra(
                    "EXTRA_ACTIVIDAD",
                    actividadSeleccionada
                )
            }

            startActivity(siguientePantalla)
        }

        actualizarValores()
        validarFormularioCompleto()
    }

    // ACTUALIZAR LOS DATOS VISIBLES
    private fun actualizarValores() {

        sexoSeleccionado?.let { valor ->

            tvValorSexo.text = getString(
                R.string.formato_valor_seleccionado,
                valor
            )

            marcarSeleccionado(tvValorSexo)
        }

        edadSeleccionada?.let { valor ->

            tvValorEdad.text = getString(
                R.string.formato_edad_valor,
                valor
            )

            marcarSeleccionado(tvValorEdad)
        }

        alturaSeleccionada?.let { valor ->

            tvValorAltura.text = getString(
                R.string.formato_altura_valor,
                valor
            )

            marcarSeleccionado(tvValorAltura)
        }

        pesoSeleccionado?.let { valor ->

            tvValorPeso.text = getString(
                R.string.formato_peso_valor,
                valor
            )

            marcarSeleccionado(tvValorPeso)
        }

        actividadSeleccionada?.let { valor ->

            val textoRes = when (valor) {

                ActividadDialog.Companion.SEDENTARIO ->
                    R.string.actividad_sedentario

                ActividadDialog.Companion.LIGERO ->
                    R.string.actividad_ligero

                ActividadDialog.Companion.MODERADO ->
                    R.string.actividad_moderado

                ActividadDialog.Companion.MUY_ACTIVO ->
                    R.string.actividad_muy_activo

                ActividadDialog.Companion.EXTREMO ->
                    R.string.actividad_extremo

                else ->
                    R.string.accion_seleccionar
            }

            tvValorActividad.text = getString(
                R.string.formato_valor_seleccionado,
                getString(textoRes)
            )

            marcarSeleccionado(tvValorActividad)
        }
    }

    // COLOR DEL CAMPO SELECCIONADO
    private fun marcarSeleccionado(campo: TextView) {

        campo.setTextColor(
            ContextCompat.getColor(
                this,
                R.color.color_valor_seleccionado
            )
        )
    }

    // COMPROBAR QUE TODOS LOS CAMPOS ESTÉN COMPLETOS
    private fun formularioCompleto(): Boolean {

        return sexoSeleccionado != null &&
                edadSeleccionada != null &&
                fechaNacimientoSeleccionada != null &&
                alturaSeleccionada != null &&
                pesoSeleccionado != null &&
                actividadSeleccionada != null
    }

    // HABILITAR O DESHABILITAR CONTINUAR
    private fun validarFormularioCompleto() {

        val valido = formularioCompleto()

        btnContinuar.isEnabled = valido

        val colorFondo = if (valido) {
            R.color.color_boton
        } else {
            R.color.color_boton_deshabilitado
        }

        val colorTexto = if (valido) {
            R.color.colors
        } else {
            R.color.color_texto_deshabilitado
        }

        btnContinuar.backgroundTintList =
            ColorStateList.valueOf(
                ContextCompat.getColor(
                    this,
                    colorFondo
                )
            )

        btnContinuar.setTextColor(
            ContextCompat.getColor(
                this,
                colorTexto
            )
        )
    }

    // GUARDAR VALORES ANTES DE RECREAR LA ACTIVITY
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        sexoSeleccionado?.let {
            outState.putString(KEY_SEXO, it)
        }

        edadSeleccionada?.let {
            outState.putInt(KEY_EDAD, it)
        }

        // NUEVO
        fechaNacimientoSeleccionada?.let {
            outState.putString(
                KEY_FECHA_NACIMIENTO,
                it
            )
        }

        alturaSeleccionada?.let {
            outState.putInt(KEY_ALTURA, it)
        }

        pesoSeleccionado?.let {
            outState.putDouble(KEY_PESO, it)
        }

        actividadSeleccionada?.let {
            outState.putString(KEY_ACTIVIDAD, it)
        }
    }

    companion object {

        private const val KEY_SEXO = "estado_sexo"
        private const val KEY_EDAD = "estado_edad"
        private const val KEY_FECHA_NACIMIENTO = "estado_fecha_nacimiento"
        private const val KEY_ALTURA = "estado_altura"
        private const val KEY_PESO = "estado_peso"
        private const val KEY_ACTIVIDAD = "estado_actividad"
    }
}