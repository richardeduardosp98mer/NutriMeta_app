package com.app.nutrimeta

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var indicators: List<View>

    private val frases = listOf(
        R.string.carrusel_frase_1,
        R.string.carrusel_frase_2,
        R.string.carrusel_frase_3
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val viewPager = findViewById<ViewPager2>(R.id.carrusel)

        val textoCarrusel = findViewById<TextView>(R.id.textoCarrusel)
        textoCarrusel.setText(frases[0])

        //configuracion del boton iniciar sesion
        val linkIniciarSesion = findViewById<TextView>(R.id.linkIniciarSesion)

        linkIniciarSesion.setOnClickListener {
            LoginDialogFragment().show(supportFragmentManager, "LoginDialog")
        }
        //////

        indicators = listOf(
            findViewById(R.id.indicador1),
            findViewById(R.id.indicador2),
            findViewById(R.id.indicador3)
        )

        val videoList = listOf(
            R.raw.plato_uno,
            R.raw.plato_uno,
            R.raw.plato_uno
        )

        // Adaptador con callback que avanza a la siguiente tarjeta al terminar el video
        val adapter = VideoCarouselAdapter(videoList) {
            val nextItem = (viewPager.currentItem + 1) % videoList.size
            viewPager.setCurrentItem(nextItem, true)
        }
        viewPager.adapter = adapter
        viewPager.offscreenPageLimit = 3

        // Efecto visual de escala y transparencia
        viewPager.setPageTransformer { page, position ->
            val scale = 0.85f + (1 - 0.85f) * (1 - abs(position))
            page.scaleY = scale
            page.alpha = 0.5f + (1 - 0.5f) * (1 - abs(position))
        }

        // Listener para actualizar las líneas y reproducir el video activo
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateIndicators(position)
                adapter.playVideoAt(position)

                textoCarrusel.setText(frases[position])
            }
        })
    }

    private fun updateIndicators(selectedPosition: Int) {
        val density = resources.displayMetrics.density
        indicators.forEachIndexed { index, view ->
            val isCurrent = index == selectedPosition
            view.setBackgroundResource(
                if (isCurrent) R.drawable.indicador_activo else R.drawable.indicador_inactivo
            )

            // La línea activa es más ancha (30dp) y las inactivas son más cortas (14dp)
            val layoutParams = view.layoutParams
            layoutParams.width = ((if (isCurrent) 30 else 14) * density).toInt()
            view.layoutParams = layoutParams
        }
    }
}