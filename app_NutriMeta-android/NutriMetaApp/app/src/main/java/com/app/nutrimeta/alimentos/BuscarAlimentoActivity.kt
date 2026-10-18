package com.app.nutrimeta.alimentos

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.nutrimeta.R
import com.app.nutrimeta.data.local.AlimentoDb
import com.app.nutrimeta.data.local.AlimentoRepositorio
import com.google.firebase.auth.FirebaseAuth

// Busca un alimento y lo abre en el detalle para elegir la cantidad.
// No guarda nada: cuando el usuario confirma en el detalle, esta pantalla
// se cierra y devuelve a PlatoActivity el id del alimento y la cantidad.
class BuscarAlimentoActivity : AppCompatActivity() {

    companion object {
        private const val ESPERA_ESCRITURA_MS = 250L
    }

    private lateinit var uidUsuario: String
    private lateinit var repositorio: AlimentoRepositorio
    private lateinit var adaptador: AlimentoAdapter

    private lateinit var etBuscar: EditText
    private lateinit var tvEstado: TextView
    private lateinit var rvAlimentos: RecyclerView

    private val manejador = Handler(Looper.getMainLooper())
    private var busquedaPendiente: Runnable? = null
    private var numeroBusqueda = 0

    // Si el detalle confirmo un ingrediente, se reenvia tal cual al plato.
    private val lanzadorDetalle = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resultado ->
        if (resultado.resultCode == RESULT_OK) {
            setResult(RESULT_OK, resultado.data)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_buscar_alimento)

        configurarBarrasSistema()

        val usuario = FirebaseAuth.getInstance().currentUser

        if (usuario == null) {
            finish()
            return
        }

        uidUsuario = usuario.uid
        repositorio = AlimentoRepositorio(applicationContext)

        vincularVistas()
        configurarLista()

        etBuscar.doAfterTextChanged { texto ->
            programarBusqueda(texto?.toString().orEmpty(), ESPERA_ESCRITURA_MS)
        }

        // Primera carga: la primera vez tambien prepara el catalogo local.
        programarBusqueda("", 0L)
    }

    private fun configurarBarrasSistema() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val raiz = findViewById<View>(R.id.rootBuscarAlimento)

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
        etBuscar = findViewById(R.id.etBuscarAlimento)
        tvEstado = findViewById(R.id.tvEstadoBuscar)
        rvAlimentos = findViewById(R.id.rvAlimentos)

        findViewById<View>(R.id.btnVolverBuscar).setOnClickListener {
            finish()
        }
    }

    private fun configurarLista() {
        adaptador = AlimentoAdapter { alimento ->
            abrirDetalle(alimento)
        }

        rvAlimentos.layoutManager = LinearLayoutManager(this)
        rvAlimentos.adapter = adaptador
    }

    // Espera un momento despues de la ultima tecla para no consultar en cada letra.
    private fun programarBusqueda(texto: String, esperaMs: Long) {

        busquedaPendiente?.let { manejador.removeCallbacks(it) }

        val tarea = Runnable { buscar(texto) }

        busquedaPendiente = tarea
        manejador.postDelayed(tarea, esperaMs)
    }

    private fun buscar(texto: String) {

        val numero = ++numeroBusqueda

        Thread {

            val resultado = runCatching {
                repositorio.buscar(uidUsuario, texto)
            }

            runOnUiThread {

                // Se ignora si la pantalla se cerro o ya hay una busqueda mas nueva.
                if (isFinishing || isDestroyed || numero != numeroBusqueda) {
                    return@runOnUiThread
                }

                resultado.fold(
                    onSuccess = { lista ->
                        mostrar(lista)
                    },
                    onFailure = {
                        adaptador.submitList(emptyList())
                        tvEstado.visibility = View.VISIBLE
                        tvEstado.setText(R.string.ali_error_busqueda)
                    }
                )
            }

        }.start()
    }

    private fun mostrar(lista: List<AlimentoDb>) {

        adaptador.submitList(lista)

        if (lista.isEmpty()) {
            tvEstado.visibility = View.VISIBLE
            tvEstado.setText(R.string.ali_sin_resultados)
        } else {
            tvEstado.visibility = View.GONE
        }
    }

    private fun abrirDetalle(alimento: AlimentoDb) {

        val intentDetalle = Intent(this, DetalleAlimentoActivity::class.java).apply {
            putExtra(DetalleAlimentoActivity.EXTRA_ID_ALIMENTO, alimento.id)
            putExtra(DetalleAlimentoActivity.EXTRA_MODO_SELECCION, true)
        }

        lanzadorDetalle.launch(intentDetalle)
    }

    override fun onDestroy() {
        busquedaPendiente?.let { manejador.removeCallbacks(it) }
        super.onDestroy()
    }
}
