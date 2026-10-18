package com.app.nutrimeta.alimentos

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import com.app.nutrimeta.R
import com.app.nutrimeta.data.local.AlimentoDb
import com.app.nutrimeta.data.local.AlimentoRepositorio
import com.app.nutrimeta.data.local.RegistroComidaRepositorio
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth

// Muestra los valores del alimento y calcula el aporte de una cantidad.
// No guarda nada: en modo seleccion devuelve el id del alimento y la
// cantidad elegida a quien lo abrio (BuscarAlimentoActivity o PlatoActivity).
class DetalleAlimentoActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID_ALIMENTO = "id_alimento"

        // Entrada: cantidad con la que arranca la pantalla. Salida: la elegida.
        const val EXTRA_CANTIDAD = "cantidad"

        // true: muestra el boton para devolver el alimento al plato.
        const val EXTRA_MODO_SELECCION = "modo_seleccion"

        // true: el ingrediente ya estaba en el plato (el boton dice "Actualizar").
        const val EXTRA_EDITANDO = "editando"

        // Posicion del ingrediente en el plato (-1 si es nuevo). Se devuelve igual.
        const val EXTRA_INDICE = "indice"

        private const val CANTIDAD_INICIAL = 100.0
    }

    private lateinit var uidUsuario: String

    private var alimento: AlimentoDb? = null
    private var modoSeleccion = false
    private var indice = -1

    private lateinit var tvNombre: TextView
    private lateinit var tvGrupo: TextView
    private lateinit var tvBase: TextView
    private lateinit var tvEnergia: TextView
    private lateinit var tvProteinas: TextView
    private lateinit var tvCarbohidratos: TextView
    private lateinit var tvGrasas: TextView
    private lateinit var tvFibra: TextView
    private lateinit var etCantidad: EditText
    private lateinit var tvUnidad: TextView
    private lateinit var tvErrorCantidad: TextView
    private lateinit var tvAporteKcal: TextView
    private lateinit var tvAporteMacros: TextView
    private lateinit var tvFuente: TextView
    private lateinit var btnAgregar: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_alimento)

        configurarBarrasSistema()
        vincularVistas()

        val usuario = FirebaseAuth.getInstance().currentUser
        val idAlimento = intent.getLongExtra(EXTRA_ID_ALIMENTO, -1L)

        if (usuario == null || idAlimento < 0L) {
            finish()
            return
        }

        uidUsuario = usuario.uid

        modoSeleccion = intent.getBooleanExtra(EXTRA_MODO_SELECCION, false)
        indice = intent.getIntExtra(EXTRA_INDICE, -1)

        val cantidadInicial = intent
            .getDoubleExtra(EXTRA_CANTIDAD, CANTIDAD_INICIAL)
            .takeIf { it > 0.0 }
            ?: CANTIDAD_INICIAL

        configurarBotones(intent.getBooleanExtra(EXTRA_EDITANDO, false))

        etCantidad.setText(FormatoCantidad.texto(cantidadInicial))
        etCantidad.doAfterTextChanged { actualizarAporte() }

        cargarAlimento(idAlimento)
    }

    private fun configurarBarrasSistema() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val raiz = findViewById<View>(R.id.rootDetalleAlimento)

        ViewCompat.setOnApplyWindowInsetsListener(raiz) { vista, insets ->

            val barras = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                        WindowInsetsCompat.Type.ime()
            )

            vista.setPadding(0, barras.top, 0, barras.bottom)

            insets
        }
    }

    private fun vincularVistas() {
        tvNombre = findViewById(R.id.tvNombreDetalle)
        tvGrupo = findViewById(R.id.tvGrupoDetalle)
        tvBase = findViewById(R.id.tvBaseDetalle)
        tvEnergia = findViewById(R.id.tvEnergiaDetalle)
        tvProteinas = findViewById(R.id.tvProteinasDetalle)
        tvCarbohidratos = findViewById(R.id.tvCarbohidratosDetalle)
        tvGrasas = findViewById(R.id.tvGrasasDetalle)
        tvFibra = findViewById(R.id.tvFibraDetalle)
        etCantidad = findViewById(R.id.etCantidad)
        tvUnidad = findViewById(R.id.tvUnidadCantidad)
        tvErrorCantidad = findViewById(R.id.tvErrorCantidad)
        tvAporteKcal = findViewById(R.id.tvAporteKcal)
        tvAporteMacros = findViewById(R.id.tvAporteMacros)
        tvFuente = findViewById(R.id.tvFuenteDetalle)
        btnAgregar = findViewById(R.id.btnAgregarAlimento)
    }

    private fun configurarBotones(editando: Boolean) {

        findViewById<View>(R.id.btnVolverDetalle).setOnClickListener {
            finish()
        }

        // Sin modo seleccion la pantalla solo muestra informacion.
        if (!modoSeleccion) {
            btnAgregar.visibility = View.GONE
            return
        }

        btnAgregar.setText(
            if (editando) {
                R.string.ali_actualizar_cantidad
            } else {
                R.string.ali_agregar_al_plato
            }
        )

        btnAgregar.setOnClickListener {
            devolverSeleccion()
        }
    }

    private fun cargarAlimento(idAlimento: Long) {

        Thread {

            val resultado = runCatching {
                AlimentoRepositorio(applicationContext)
                    .obtenerPorId(uidUsuario, idAlimento)
            }

            runOnUiThread {

                if (isFinishing || isDestroyed) {
                    return@runOnUiThread
                }

                val encontrado = resultado.getOrNull()

                if (encontrado == null) {
                    Toast.makeText(
                        this,
                        R.string.ali_error_cargar,
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()
                } else {
                    alimento = encontrado
                    mostrar(encontrado)
                }
            }

        }.start()
    }

    private fun mostrar(a: AlimentoDb) {

        tvNombre.text = a.nombre
        tvGrupo.text = a.grupo

        tvBase.text = getString(R.string.ali_por_100, a.unidad)
        tvEnergia.text = getString(R.string.ali_kcal, a.energiaKcal)

        tvProteinas.text = getString(R.string.ali_gramos, a.proteinaG)
        tvCarbohidratos.text = getString(R.string.ali_gramos, a.carbohidratosG)
        tvGrasas.text = getString(R.string.ali_gramos, a.grasaG)

        // Sin dato no es lo mismo que 0 g de fibra.
        val fibra = a.fibraG

        tvFibra.text = if (fibra == null) {
            getString(R.string.ali_fibra_sin_dato)
        } else {
            getString(R.string.ali_fibra, fibra)
        }

        tvUnidad.text = a.unidad

        tvFuente.text = getString(
            R.string.ali_fuente,
            getString(textoFuente(a.fuente))
        )

        actualizarAporte()
    }

    private fun textoFuente(fuente: String): Int {
        return when (fuente) {
            "INS" -> R.string.ali_fuente_ins
            "INS_IMPUTADO" -> R.string.ali_fuente_ins_imputado
            "ETIQUETA" -> R.string.ali_fuente_etiqueta
            else -> R.string.ali_fuente_admin
        }
    }

    // Devuelve la cantidad escrita, o null si no es un numero entre 0 y el maximo.
    private fun cantidadValida(): Double? {

        val valor = etCantidad.text.toString()
            .trim()
            .replace(',', '.')
            .toDoubleOrNull()
            ?: return null

        return if (valor > 0.0 && valor <= RegistroComidaRepositorio.CANTIDAD_MAXIMA) {
            valor
        } else {
            null
        }
    }

    // Recalcula lo que aporta la cantidad escrita (los valores son por 100 g o ml).
    private fun actualizarAporte() {

        val a = alimento ?: return
        val cantidad = cantidadValida()

        if (cantidad == null) {
            tvErrorCantidad.visibility = View.VISIBLE
            tvErrorCantidad.text = getString(R.string.ali_cantidad_invalida, a.unidad)
            tvAporteKcal.setText(R.string.ali_sin_aporte)
            tvAporteMacros.text = ""
            habilitarBoton(false)
            return
        }

        val factor = cantidad / 100.0

        tvErrorCantidad.visibility = View.GONE

        tvAporteKcal.text = getString(R.string.ali_kcal, a.energiaKcal * factor)

        tvAporteMacros.text = getString(
            R.string.ali_aporte_macros,
            a.proteinaG * factor,
            a.carbohidratosG * factor,
            a.grasaG * factor
        )

        habilitarBoton(true)
    }

    // El color del boton es fijo, asi que se atenua a mano cuando esta deshabilitado.
    private fun habilitarBoton(habilitado: Boolean) {
        btnAgregar.isEnabled = habilitado
        btnAgregar.alpha = if (habilitado) 1f else 0.4f
    }

    private fun devolverSeleccion() {

        val a = alimento ?: return
        val cantidad = cantidadValida() ?: return

        val resultado = Intent().apply {
            putExtra(EXTRA_ID_ALIMENTO, a.id)
            putExtra(EXTRA_CANTIDAD, cantidad)
            putExtra(EXTRA_INDICE, indice)
        }

        setResult(RESULT_OK, resultado)
        finish()
    }
}
