package com.app.nutrimeta.alimentos

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.nutrimeta.R
import com.app.nutrimeta.data.local.RegistroComidaRepositorio
import com.app.nutrimeta.data.local.ResumenPlato
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth

// Platos guardados de una comida (por ejemplo, el desayuno) en un dia: desde aqui
// se puede editar un plato (reabre PlatoActivity), eliminarlo o agregar otro.
class HistorialComidaActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID_TIPO_COMIDA = "id_tipo_comida"
        const val EXTRA_NOMBRE_COMIDA = "nombre_comida"
        const val EXTRA_FECHA = "fecha"
    }

    private lateinit var uidUsuario: String
    private var idTipoComida = 0
    private var nombreComida = ""
    private var fecha = ""

    private var numeroCarga = 0

    private lateinit var adaptador: PlatoHistorialAdapter
    private lateinit var rvPlatos: RecyclerView
    private lateinit var tvVacio: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial_comida)

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

        findViewById<TextView>(R.id.tvTituloHistorial).text =
            getString(R.string.hist_titulo, nombreComida, fecha)

        findViewById<View>(R.id.btnVolverHistorial).setOnClickListener {
            finish()
        }

        tvVacio = findViewById(R.id.tvVacioHistorial)
        rvPlatos = findViewById(R.id.rvPlatosHistorial)

        adaptador = PlatoHistorialAdapter(
            alEditar = { plato -> abrirPlato(plato.idRegistro) },
            alEliminar = { plato -> confirmarEliminar(plato) }
        )

        rvPlatos.layoutManager = LinearLayoutManager(this)
        rvPlatos.adapter = adaptador

        findViewById<View>(R.id.btnAgregarPlatoHistorial).setOnClickListener {
            abrirPlato(-1L)
        }
    }

    // Al volver de editar o agregar, la lista se recarga.
    override fun onResume() {
        super.onResume()
        cargar()
    }

    private fun configurarBarrasSistema() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val raiz = findViewById<View>(R.id.rootHistorial)

        ViewCompat.setOnApplyWindowInsetsListener(raiz) { vista, insets ->

            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            vista.setPadding(0, barras.top, 0, barras.bottom)

            insets
        }
    }

    // idRegistro = -1 agrega un plato nuevo; otro valor edita ese plato.
    private fun abrirPlato(idRegistro: Long) {

        startActivity(
            Intent(this, PlatoActivity::class.java).apply {
                putExtra(PlatoActivity.EXTRA_ID_TIPO_COMIDA, idTipoComida)
                putExtra(PlatoActivity.EXTRA_NOMBRE_COMIDA, nombreComida)
                putExtra(PlatoActivity.EXTRA_FECHA, fecha)
                putExtra(PlatoActivity.EXTRA_ID_REGISTRO, idRegistro)
            }
        )
    }

    private fun cargar() {

        val cargaActual = ++numeroCarga

        Thread {

            val resultado = runCatching {
                RegistroComidaRepositorio(applicationContext)
                    .listarPlatos(uidUsuario, idTipoComida, fecha)
            }

            runOnUiThread {

                if (isFinishing || isDestroyed || cargaActual != numeroCarga) {
                    return@runOnUiThread
                }

                resultado.fold(
                    onSuccess = { platos ->
                        adaptador.actualizar(platos)

                        val hayPlatos = platos.isNotEmpty()
                        tvVacio.visibility = if (hayPlatos) View.GONE else View.VISIBLE
                        rvPlatos.visibility = if (hayPlatos) View.VISIBLE else View.GONE
                    },
                    onFailure = {
                        Toast.makeText(this, R.string.hist_error_cargar, Toast.LENGTH_SHORT).show()
                    }
                )
            }

        }.start()
    }

    private fun confirmarEliminar(plato: ResumenPlato) {

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.hist_eliminar_titulo)
            .setMessage(R.string.hist_eliminar_mensaje)
            .setPositiveButton(R.string.hist_eliminar) { _, _ -> eliminar(plato) }
            .setNegativeButton(R.string.hist_cancelar, null)
            .show()
    }

    private fun eliminar(plato: ResumenPlato) {

        Thread {

            val resultado = runCatching {
                RegistroComidaRepositorio(applicationContext)
                    .eliminarPlato(uidUsuario, plato.idRegistro)
            }

            runOnUiThread {

                if (isFinishing || isDestroyed) {
                    return@runOnUiThread
                }

                if (resultado.isSuccess) {
                    Toast.makeText(this, R.string.hist_eliminado, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, R.string.hist_error_eliminar, Toast.LENGTH_SHORT).show()
                }

                cargar()
            }

        }.start()
    }
}
