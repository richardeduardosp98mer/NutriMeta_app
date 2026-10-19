package com.app.nutrimeta.onboarding

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.app.nutrimeta.R
import com.app.nutrimeta.auth.RegistroActivity
import com.app.nutrimeta.domain.CalculadoraMetas
import com.app.nutrimeta.domain.CalculadoraNutricional
import com.app.nutrimeta.domain.DatosCalculo
import com.app.nutrimeta.domain.LimitesSeguros
import com.app.nutrimeta.domain.ObjetivoNutricional
import com.app.nutrimeta.domain.SexoBiologico
import com.google.android.material.button.MaterialButton
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

// Paso del onboarding solo para quienes quieren bajar o subir de peso:
// pide el peso objetivo y el plazo en semanas, y muestra si la meta es segura.
// Despues pasa a RegistroActivity con los mismos datos mas estos dos.
class MetaActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PESO_OBJETIVO = "EXTRA_PESO_OBJETIVO"
        const val EXTRA_PLAZO_SEMANAS = "EXTRA_PLAZO_SEMANAS"

        private const val PESO_MINIMO = 30.0
        private const val PESO_MAXIMO = 300.0

        // Mismos factores que el catalogo de niveles de actividad; solo para la vista previa.
        // El calculo que se guarda usa el factor del catalogo local.
        private val FACTORES_ACTIVIDAD = mapOf(
            "SEDENTARIO" to 1.200,
            "LIGERO" to 1.375,
            "MODERADO" to 1.550,
            "MUY_ACTIVO" to 1.725,
            "EXTREMO" to 1.900
        )
    }

    private sealed class Estado {
        object Vacio : Estado()
        class Error(val mensaje: String) : Estado()
        class Valido(val pesoObjetivo: Double, val plazoSemanas: Int, val resumen: String) : Estado()
    }

    private lateinit var etPesoObjetivo: EditText
    private lateinit var etPlazo: EditText
    private lateinit var tvResumen: TextView
    private lateinit var btnContinuar: MaterialButton

    private var objetivo = ObjetivoNutricional.BAJAR
    private var peso = 0.0
    private var sexo: SexoBiologico? = null
    private var gastoDiarioKcal: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meta)

        objetivo = if (intent.getStringExtra("EXTRA_OBJETIVO") == "GANAR_MUSCULO") {
            ObjetivoNutricional.SUBIR
        } else {
            ObjetivoNutricional.BAJAR
        }

        peso = intent.getDoubleExtra("EXTRA_PESO", 0.0)

        sexo = when (intent.getStringExtra("EXTRA_SEXO").orEmpty().trim().lowercase(Locale.ROOT)) {
            "hombre", "m" -> SexoBiologico.M
            "mujer", "f" -> SexoBiologico.F
            else -> null
        }

        gastoDiarioKcal = calcularGastoDiario()

        etPesoObjetivo = findViewById(R.id.etPesoObjetivoMeta)
        etPlazo = findViewById(R.id.etPlazoMeta)
        tvResumen = findViewById(R.id.tvResumenMeta)
        btnContinuar = findViewById(R.id.btnContinuarMeta)

        findViewById<ImageView>(R.id.btnVolverMeta).setOnClickListener {
            finish()
        }

        val observador = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                actualizar()
            }
        }

        etPesoObjetivo.addTextChangedListener(observador)
        etPlazo.addTextChangedListener(observador)

        btnContinuar.setOnClickListener {

            val estado = evaluar()

            if (estado !is Estado.Valido) {
                return@setOnClickListener
            }

            startActivity(
                Intent(this, RegistroActivity::class.java).apply {
                    putExtras(this@MetaActivity.intent)
                    putExtra(EXTRA_PESO_OBJETIVO, estado.pesoObjetivo)
                    putExtra(EXTRA_PLAZO_SEMANAS, estado.plazoSemanas)
                }
            )
        }

        actualizar()
    }

    private fun calcularGastoDiario(): Double? {

        val sexoActual = sexo ?: return null
        val factor = FACTORES_ACTIVIDAD[
            intent.getStringExtra("EXTRA_ACTIVIDAD").orEmpty().trim().uppercase(Locale.ROOT)
        ] ?: return null

        return runCatching {
            CalculadoraNutricional.calcular(
                DatosCalculo(
                    sexo = sexoActual,
                    edad = intent.getIntExtra("EXTRA_EDAD", 0),
                    pesoKg = peso,
                    tallaCm = intent.getIntExtra("EXTRA_ALTURA", 0).toDouble(),
                    factorActividad = factor
                )
            ).gastoDiarioKcal
        }.getOrNull()
    }

    private fun evaluar(): Estado {

        val pesoObjetivo = etPesoObjetivo.text.toString().trim().replace(',', '.').toDoubleOrNull()
        val plazo = etPlazo.text.toString().trim().toIntOrNull()

        if (pesoObjetivo == null || plazo == null) {
            return Estado.Vacio
        }

        if (pesoObjetivo < PESO_MINIMO || pesoObjetivo > PESO_MAXIMO) {
            return Estado.Error(getString(R.string.meta_error_peso_rango))
        }

        if (objetivo == ObjetivoNutricional.BAJAR && pesoObjetivo >= peso) {
            return Estado.Error(getString(R.string.meta_error_peso_bajar, formatoPeso(peso)))
        }

        if (objetivo == ObjetivoNutricional.SUBIR && pesoObjetivo <= peso) {
            return Estado.Error(getString(R.string.meta_error_peso_subir, formatoPeso(peso)))
        }

        if (plazo < 1 || plazo > LimitesSeguros.PLAZO_MAXIMO_SEMANAS) {
            return Estado.Error(getString(R.string.meta_error_plazo))
        }

        val gasto = gastoDiarioKcal
        val sexoActual = sexo

        if (gasto == null || sexoActual == null) {
            return Estado.Valido(pesoObjetivo, plazo, "")
        }

        val resultado = runCatching {
            CalculadoraMetas.calcularConMeta(
                gastoDiarioKcal = gasto,
                pesoKg = peso,
                objetivo = objetivo,
                sexo = sexoActual,
                pesoObjetivoKg = pesoObjetivo,
                plazoSemanas = plazo
            )
        }.getOrNull() ?: return Estado.Valido(pesoObjetivo, plazo, "")

        val ritmo = abs(pesoObjetivo - peso) / plazo
        val kcal = resultado.metas.metaKcal.roundToInt()

        val resumen = if (resultado.ajustada) {
            getString(
                R.string.meta_resumen_ajustada,
                ritmo,
                kcal,
                resultado.semanasReales ?: plazo
            )
        } else {
            getString(R.string.meta_resumen_ok, ritmo, kcal)
        }

        return Estado.Valido(pesoObjetivo, plazo, resumen)
    }

    private fun actualizar() {

        when (val estado = evaluar()) {

            is Estado.Vacio -> {
                tvResumen.visibility = View.GONE
                habilitarContinuar(false)
            }

            is Estado.Error -> {
                tvResumen.text = estado.mensaje
                tvResumen.setTextColor(ContextCompat.getColor(this, R.color.meta_error))
                tvResumen.visibility = View.VISIBLE
                habilitarContinuar(false)
            }

            is Estado.Valido -> {
                tvResumen.text = estado.resumen
                tvResumen.setTextColor(ContextCompat.getColor(this, R.color.blanco))
                tvResumen.visibility = if (estado.resumen.isBlank()) View.GONE else View.VISIBLE
                habilitarContinuar(true)
            }
        }
    }

    private fun habilitarContinuar(habilitado: Boolean) {

        btnContinuar.isEnabled = habilitado

        btnContinuar.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(
                this,
                if (habilitado) R.color.color_boton else R.color.color_boton_deshabilitado
            )
        )

        btnContinuar.setTextColor(
            ContextCompat.getColor(
                this,
                if (habilitado) R.color.colors else R.color.color_texto_deshabilitado
            )
        )
    }

    private fun formatoPeso(valor: Double): String {
        return String.format(Locale.US, "%.1f", valor)
    }
}
