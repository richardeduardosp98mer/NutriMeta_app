package com.app.nutrimeta

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.card.MaterialCardView

class ObjetivoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_objetivo)

        val cardPerderGrasa = findViewById<MaterialCardView>(R.id.cardPerderGrasa)
        val cardGanarMusculo = findViewById<MaterialCardView>(R.id.cardGanarMusculo)
        val cardMantenerPeso = findViewById<MaterialCardView>(R.id.cardMantenerPeso)

        cardPerderGrasa.setOnClickListener {
            irASiguiente("PERDER_GRASA")
        }

        cardGanarMusculo.setOnClickListener {
            irASiguiente("GANAR_MUSCULO")
        }

        cardMantenerPeso.setOnClickListener {
            irASiguiente("MANTENER_PESO")
        }
    }

    private fun irASiguiente(objetivo: String) {
        val intent = Intent(this, CalculoCaloriasActivity::class.java).apply {
            putExtra("EXTRA_OBJETIVO", objetivo)
        }
        startActivity(intent)
    }
}