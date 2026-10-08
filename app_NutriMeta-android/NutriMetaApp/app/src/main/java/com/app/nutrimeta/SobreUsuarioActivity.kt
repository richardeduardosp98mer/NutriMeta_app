package com.app.nutrimeta

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class SobreUsuarioActivity : AppCompatActivity() {

    private var sexoSeleccionado: String? = null
    private var edadSeleccionada: Int? = null
    private var alturaSeleccionada: Int? = null
    private var pesoSeleccionado: Double? = null
    private lateinit var btnContinuar: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sobre_usuario)

        // 1. Recibir el objetivo que viene desde la pantalla anterior
        val objetivo = intent.getStringExtra("EXTRA_OBJETIVO") ?: "PERDER_GRASA"

        val btnVolver = findViewById<ImageView>(R.id.btnVolverSobreTi)
        btnContinuar = findViewById(R.id.btnContinuarSobreTi)

        val cardSexo = findViewById<MaterialCardView>(R.id.cardSexo)
        val cardEdad = findViewById<MaterialCardView>(R.id.cardEdad)
        val cardAltura = findViewById<MaterialCardView>(R.id.cardAltura)
        val cardPeso = findViewById<MaterialCardView>(R.id.cardPeso)

        val tvValorSexo = findViewById<TextView>(R.id.tvValorSexo)
        val tvValorEdad = findViewById<TextView>(R.id.tvValorEdad)
        val tvValorAltura = findViewById<TextView>(R.id.tvValorAltura)
        val tvValorPeso = findViewById<TextView>(R.id.tvValorPeso)

        btnVolver.setOnClickListener {
            finish()
        }

        cardSexo.setOnClickListener {
            SexoDialog(sexoSeleccionado) { seleccion ->
                sexoSeleccionado = seleccion
                tvValorSexo.text = getString(R.string.formato_valor_seleccionado, seleccion)
                tvValorSexo.setTextColor(ContextCompat.getColor(this, R.color.color_valor_seleccionado))
                validarFormularioCompleto()
            }.show(supportFragmentManager, "SexoDialog")
        }

        cardEdad.setOnClickListener {
            EdadDialog(edadPrevia = edadSeleccionada) { edad ->
                edadSeleccionada = edad
                tvValorEdad.text = getString(R.string.formato_edad_valor, edad)
                tvValorEdad.setTextColor(ContextCompat.getColor(this, R.color.color_valor_seleccionado))
                validarFormularioCompleto()
            }.show(supportFragmentManager, "EdadDialog")
        }

        cardAltura.setOnClickListener {
            AlturaDialog(alturaPrevia = alturaSeleccionada) { altura ->
                alturaSeleccionada = altura
                tvValorAltura.text = getString(R.string.formato_altura_valor, altura)
                tvValorAltura.setTextColor(ContextCompat.getColor(this, R.color.color_valor_seleccionado))
                validarFormularioCompleto()
            }.show(supportFragmentManager, "AlturaDialog")
        }

        cardPeso.setOnClickListener {
            PesoDialog(pesoPrevio = pesoSeleccionado) { peso ->
                pesoSeleccionado = peso
                tvValorPeso.text = getString(R.string.formato_peso_valor, peso)
                tvValorPeso.setTextColor(ContextCompat.getColor(this, R.color.color_valor_seleccionado))
                validarFormularioCompleto()
            }.show(supportFragmentManager, "PesoDialog")
        }

        // 2. Enviar todos los datos recolectados a la pantalla de Auth
        btnContinuar.setOnClickListener {
            val intent = Intent(this, RegistroActivity::class.java).apply {
                putExtra("EXTRA_OBJETIVO", objetivo)
                putExtra("EXTRA_SEXO", sexoSeleccionado)
                putExtra("EXTRA_EDAD", edadSeleccionada ?: 0)
                putExtra("EXTRA_ALTURA", alturaSeleccionada ?: 0)
                putExtra("EXTRA_PESO", pesoSeleccionado ?: 0.0)
            }
            startActivity(intent)
        }
    }

    private fun validarFormularioCompleto() {
        val formularioValido = sexoSeleccionado != null &&
                edadSeleccionada != null &&
                alturaSeleccionada != null &&
                pesoSeleccionado != null

        btnContinuar.isEnabled = formularioValido

        if (formularioValido) {
            val colorActivo = ContextCompat.getColor(this, R.color.color_boton)
            btnContinuar.backgroundTintList = ColorStateList.valueOf(colorActivo)
            btnContinuar.setTextColor(ContextCompat.getColor(this, R.color.colors))
        } else {
            val colorInactivo = ContextCompat.getColor(this, R.color.color_boton_deshabilitado)
            btnContinuar.backgroundTintList = ColorStateList.valueOf(colorInactivo)
            btnContinuar.setTextColor(ContextCompat.getColor(this, R.color.color_texto_deshabilitado))
        }
    }
}