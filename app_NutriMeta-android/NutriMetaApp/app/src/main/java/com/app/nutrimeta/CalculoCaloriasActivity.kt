package com.app.nutrimeta

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class CalculoCaloriasActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calculo_calorias)

        val btnVolver = findViewById<ImageView>(R.id.btnVolver)
        val tvIcono = findViewById<TextView>(R.id.tvIconoNucleo)
        val btnContinuar = findViewById<MaterialButton>(R.id.btnContinuar)

        btnVolver.setOnClickListener {
            finish()
        }

        // Recibimos el objetivo seleccionado para personalizar el ícono
        val objetivo = intent.getStringExtra("EXTRA_OBJETIVO") ?: "PERDER_GRASA"
        when (objetivo) {
            "PERDER_GRASA" -> tvIcono.setText(R.string.emoji_perder_grasa)
            "GANAR_MUSCULO" -> tvIcono.setText(R.string.emoji_ganar_musculo)
            "MANTENER_PESO" -> tvIcono.setText(R.string.emoji_mantener_peso)
        }

        btnContinuar.setOnClickListener {
            val intent = Intent(this, SobreUsuarioActivity::class.java).apply {
                putExtra("EXTRA_OBJETIVO", objetivo)
            }
            startActivity(intent)
        }
    }
}