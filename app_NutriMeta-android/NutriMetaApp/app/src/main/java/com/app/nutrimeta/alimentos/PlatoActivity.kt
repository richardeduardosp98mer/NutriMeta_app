package com.app.nutrimeta.alimentos

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.nutrimeta.R
import com.app.nutrimeta.data.local.AlimentoRepositorio
import com.app.nutrimeta.data.local.IngredientePlato
import com.app.nutrimeta.data.local.RegistroComidaRepositorio
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth

// Arma un plato (por ejemplo, un desayuno) ingrediente por ingrediente.
// Nada se guarda hasta tocar "Guardar": entonces se crea un solo
// registro_comida con un detalle_registro por ingrediente.
class PlatoActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID_TIPO_COMIDA = "id_tipo_comida"
        const val EXTRA_NOMBRE_COMIDA = "nombre_comida"
        const val EXTRA_FECHA = "fecha"

        private const val ESTADO_INGREDIENTES = "ingredientes"
    }

    private lateinit var uidUsuario: String
    private var idTipoComida = 0
    private var nombreComida = ""
    private var fecha = ""

    private var ingredientes = ArrayList<IngredientePlato>()
    private var guardando = false

    private lateinit var adaptador: IngredientePlatoAdapter
    private lateinit var tvTotalKcal: TextView
    private lateinit var tvTotalMacros: TextView
    private lateinit var tvVacio: TextView
    private lateinit var rvIngredientes: RecyclerView
    private lateinit var btnGuardar: MaterialButton

    // Un solo lanzador sirve para agregar (Buscar) y para editar (Detalle):
    // ambos devuelven id del alimento, cantidad e indice (-1 = nuevo).
    private val lanzador = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resultado ->
        val datos = resultado.data

        if (resultado.resultCode == RESULT_OK && datos != null) {
            recibirIngrediente(datos)
        }
    }

    @Suppress("UNCHECKED_CAST", "DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plato)

        configurarBarrasSistema()

        val usuario = FirebaseAuth.getInstance().currentUser

        idTipoComida = intent.getIntExtra(EXTRA_ID_TIPO_COMIDA, 0)
        nombreComida = intent.getStringExtra(EXTRA_NOMBRE_COMIDA).orEmpty()
        fecha = intent.getStringExtra(EXTRA_FECHA).orEmpty()

        if (usuario == null || idTipoComida == 0 || fecha.isEmpty()) {
            finish()
            return
        }

        uidUsuario = usuario.uid

        // Si la pantalla se recrea (por ejemplo al rotar) se conserva el plato.
        val guardados = savedInstanceState?.getSerializable(ESTADO_INGREDIENTES)

        if (guardados is ArrayList<*>) {
            ingredientes = guardados as ArrayList<IngredientePlato>
        }

        vincularVistas()
        configurarAtras()
        mostrar()
    }

    private fun configurarBarrasSistema() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val raiz = findViewById<View>(R.id.rootPlato)

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
        tvTotalKcal = findViewById(R.id.tvTotalKcalPlato)
        tvTotalMacros = findViewById(R.id.tvTotalMacrosPlato)
        tvVacio = findViewById(R.id.tvVacioPlato)
        rvIngredientes = findViewById(R.id.rvIngredientes)
        btnGuardar = findViewById(R.id.btnGuardarPlato)

        val nombreMinuscula = nombreComida.lowercase()

        findViewById<TextView>(R.id.tvTituloPlato).text =
            getString(R.string.ali_plato_titulo, nombreMinuscula)

        btnGuardar.text = getString(R.string.ali_plato_guardar, nombreMinuscula)

        findViewById<View>(R.id.btnVolverPlato).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        adaptador = IngredientePlatoAdapter(
            alEditar = { posicion -> editarIngrediente(posicion) },
            alQuitar = { posicion -> quitarIngrediente(posicion) }
        )

        rvIngredientes.layoutManager = LinearLayoutManager(this)
        rvIngredientes.adapter = adaptador

        findViewById<View>(R.id.btnAgregarIngrediente).setOnClickListener {
            lanzador.launch(Intent(this, BuscarAlimentoActivity::class.java))
        }

        btnGuardar.setOnClickListener {
            guardar()
        }
    }

    // Atras con ingredientes cargados pide confirmacion antes de perderlos.
    private fun configurarAtras() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {

                    if (ingredientes.isEmpty() || guardando) {
                        finish()
                        return
                    }

                    MaterialAlertDialogBuilder(this@PlatoActivity)
                        .setTitle(R.string.ali_descartar_titulo)
                        .setMessage(R.string.ali_descartar_mensaje)
                        .setPositiveButton(R.string.ali_descartar) { _, _ -> finish() }
                        .setNegativeButton(R.string.ali_seguir, null)
                        .show()
                }
            }
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(ESTADO_INGREDIENTES, ArrayList(ingredientes))
    }

    private fun recibirIngrediente(datos: Intent) {

        val cantidad = datos.getDoubleExtra(DetalleAlimentoActivity.EXTRA_CANTIDAD, 0.0)
        val indice = datos.getIntExtra(DetalleAlimentoActivity.EXTRA_INDICE, -1)
        val idAlimento = datos.getLongExtra(DetalleAlimentoActivity.EXTRA_ID_ALIMENTO, -1L)

        if (cantidad <= 0.0) {
            return
        }

        // Edicion: el alimento ya esta en el plato, solo cambia la cantidad.
        if (indice in ingredientes.indices) {
            ingredientes[indice] = ingredientes[indice].copy(cantidad = cantidad)
            mostrar()
            return
        }

        if (idAlimento < 0L) {
            return
        }

        Thread {

            val alimento = runCatching {
                AlimentoRepositorio(applicationContext)
                    .obtenerPorId(uidUsuario, idAlimento)
            }.getOrNull()

            runOnUiThread {

                if (isFinishing || isDestroyed) {
                    return@runOnUiThread
                }

                if (alimento == null) {
                    Toast.makeText(
                        this,
                        R.string.ali_error_cargar,
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    ingredientes.add(IngredientePlato(alimento, cantidad))
                    mostrar()
                }
            }

        }.start()
    }

    private fun editarIngrediente(posicion: Int) {

        val ingrediente = ingredientes.getOrNull(posicion) ?: return

        val intentDetalle = Intent(this, DetalleAlimentoActivity::class.java).apply {
            putExtra(DetalleAlimentoActivity.EXTRA_ID_ALIMENTO, ingrediente.alimento.id)
            putExtra(DetalleAlimentoActivity.EXTRA_CANTIDAD, ingrediente.cantidad)
            putExtra(DetalleAlimentoActivity.EXTRA_MODO_SELECCION, true)
            putExtra(DetalleAlimentoActivity.EXTRA_EDITANDO, true)
            putExtra(DetalleAlimentoActivity.EXTRA_INDICE, posicion)
        }

        lanzador.launch(intentDetalle)
    }

    private fun quitarIngrediente(posicion: Int) {

        if (posicion in ingredientes.indices) {
            ingredientes.removeAt(posicion)
            mostrar()
        }
    }

    // Refresca lista, totales y estado del boton Guardar.
    private fun mostrar() {

        adaptador.actualizar(ingredientes)

        val kcal = ingredientes.sumOf { it.energiaKcal }
        val proteinas = ingredientes.sumOf { it.proteinaG }
        val carbohidratos = ingredientes.sumOf { it.carbohidratosG }
        val grasas = ingredientes.sumOf { it.grasaG }

        tvTotalKcal.text = getString(R.string.ali_kcal, kcal)

        tvTotalMacros.text = getString(
            R.string.ali_aporte_macros,
            proteinas,
            carbohidratos,
            grasas
        )

        val hayIngredientes = ingredientes.isNotEmpty()

        tvVacio.visibility = if (hayIngredientes) View.GONE else View.VISIBLE
        rvIngredientes.visibility = if (hayIngredientes) View.VISIBLE else View.GONE

        val puedeGuardar = hayIngredientes && !guardando

        // El color del boton es fijo, asi que se atenua a mano.
        btnGuardar.isEnabled = puedeGuardar
        btnGuardar.alpha = if (puedeGuardar) 1f else 0.4f
    }

    private fun guardar() {

        if (ingredientes.isEmpty() || guardando) {
            return
        }

        guardando = true
        mostrar()

        val copia = ArrayList(ingredientes)

        Thread {

            val resultado = runCatching {
                RegistroComidaRepositorio(applicationContext)
                    .guardarPlato(uidUsuario, idTipoComida, fecha, copia)
            }

            runOnUiThread {

                if (isFinishing || isDestroyed) {
                    return@runOnUiThread
                }

                if (resultado.isSuccess) {
                    Toast.makeText(
                        this,
                        getString(R.string.ali_plato_guardado, nombreComida),
                        Toast.LENGTH_SHORT
                    ).show()

                    setResult(RESULT_OK)
                    finish()
                } else {
                    guardando = false
                    mostrar()

                    Toast.makeText(
                        this,
                        R.string.ali_plato_error_guardar,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        }.start()
    }
}
