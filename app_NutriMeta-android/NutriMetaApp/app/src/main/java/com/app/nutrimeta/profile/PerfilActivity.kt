package com.app.nutrimeta.profile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.app.nutrimeta.profile.ActualizacionPerfilService
import com.app.nutrimeta.profile.CambioPerfil
import com.app.nutrimeta.MainActivity
import com.app.nutrimeta.R
import com.app.nutrimeta.data.local.NutriMetaDatabaseHelper
import com.app.nutrimeta.ui.dialogs.ActividadDialog
import com.app.nutrimeta.ui.dialogs.EdadDialog
import com.app.nutrimeta.ui.dialogs.PesoDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar

class PerfilActivity : AppCompatActivity() {

    private data class FichaPerfil(
        val fechaNacimiento: String,
        val peso: Double,
        val objetivo: String,
        val actividad: String,
        val factor: Double,
        val codigoActividad: String
    )

    private var fichaActual: FichaPerfil? = null
    private var guardandoCambios = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        configurarBarrasSistema()

        findViewById<MaterialButton>(
            R.id.btnVolverPerfil
        ).setOnClickListener {
            finish()
        }

        findViewById<MaterialButton>(
            R.id.btnCerrarSesionPerfil
        ).setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(
                this,
                MainActivity::class.java
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }

        findViewById<View>(
            R.id.filaEditarEdad
        ).setOnClickListener {

            if (!guardandoCambios) {
                val ficha = fichaActual ?: return@setOnClickListener

                EdadDialog(ficha.fechaNacimiento) { fecha, _ ->

                    guardarCambio(
                        CambioPerfil.FechaNacimiento(fecha)
                    )

                }.show(
                    supportFragmentManager,
                    "EditarEdadPerfil"
                )
            }
        }

        findViewById<View>(
            R.id.filaEditarPeso
        ).setOnClickListener {

            if (!guardandoCambios) {
                val ficha = fichaActual ?: return@setOnClickListener

                PesoDialog(ficha.peso) { peso ->

                    guardarCambio(
                        CambioPerfil.Peso(peso)
                    )

                }.show(
                    supportFragmentManager,
                    "EditarPesoPerfil"
                )
            }
        }

        findViewById<View>(
            R.id.filaEditarObjetivo
        ).setOnClickListener {

            if (!guardandoCambios) {
                mostrarSelectorObjetivo()
            }
        }

        findViewById<View>(
            R.id.filaEditarActividad
        ).setOnClickListener {

            if (!guardandoCambios) {
                val ficha = fichaActual ?: return@setOnClickListener

                ActividadDialog.Companion.nuevaInstancia(
                    ficha.codigoActividad
                ).show(
                    supportFragmentManager,
                    "EditarActividadPerfil"
                )
            }
        }

        supportFragmentManager.setFragmentResultListener(
            ActividadDialog.Companion.REQUEST_KEY,
            this
        ) { _, resultado ->

            val codigo = resultado.getString(
                ActividadDialog.Companion.RESULT_KEY
            ) ?: return@setFragmentResultListener

            guardarCambio(
                CambioPerfil.Actividad(codigo)
            )
        }

        val usuario = FirebaseAuth.getInstance().currentUser

        if (usuario == null) {
            finish()
            return
        }

        val nombre = usuario.displayName
            ?.takeIf { it.isNotBlank() }
            ?: usuario.email.orEmpty()

        findViewById<TextView>(
            R.id.tvPerfilNombre
        ).text = nombre.ifBlank {
            getString(R.string.perfil_usuario)
        }

        findViewById<TextView>(
            R.id.tvPerfilCorreo
        ).text = usuario.email.orEmpty()

        findViewById<TextView>(
            R.id.tvAvatarPerfil
        ).text = nombre
            .firstOrNull()
            ?.uppercaseChar()
            ?.toString()
            ?: "?"

        cargarPerfil(usuario.uid)
    }

    private fun configurarBarrasSistema() {

        WindowCompat.setDecorFitsSystemWindows(
            window,
            false
        )

        val raiz = findViewById<View>(R.id.rootPerfil)

        ViewCompat.setOnApplyWindowInsetsListener(raiz) {
                vista, insets ->

            val barras = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )

            vista.setPadding(
                0,
                barras.top,
                0,
                barras.bottom
            )

            insets
        }
    }

    private fun cargarPerfil(uid: String) {

        val tvEstado = findViewById<TextView>(
            R.id.tvEstadoPerfil
        )

        tvEstado.setText(R.string.dashboard_cargando)

        Thread {

            val resultado = runCatching {

                NutriMetaDatabaseHelper.Companion.forUser(
                    applicationContext,
                    uid
                ).use { helper ->

                    helper.readableDatabase.rawQuery(
                        """
                        SELECT
                            p.fecha_nacimiento,
                            p.peso_kg,
                            p.objetivo,
                            n.nombre,
                            n.factor,
                            n.codigo
                        FROM perfil p
                        JOIN nivel_actividad n
                            ON p.id_nivel_actividad =
                               n.id_nivel_actividad
                        WHERE p.id = 1
                        """.trimIndent(),
                        null
                    ).use { cursor ->

                        if (cursor.moveToFirst()) {

                            FichaPerfil(
                                fechaNacimiento = cursor.getString(0),
                                peso = cursor.getDouble(1),
                                objetivo = cursor.getString(2),
                                actividad = cursor.getString(3),
                                factor = cursor.getDouble(4),
                                codigoActividad = cursor.getString(5)
                            )

                        } else {
                            null
                        }
                    }
                }
            }

            runOnUiThread {

                if (isFinishing || isDestroyed) {
                    return@runOnUiThread
                }

                resultado.fold(

                    onSuccess = { perfil ->

                        if (perfil == null) {

                            tvEstado.setText(
                                R.string.dashboard_sin_perfil
                            )

                        } else {
                            fichaActual = perfil
                            mostrarPerfil(perfil)
                            tvEstado.visibility = View.GONE
                        }
                    },

                    onFailure = {

                        tvEstado.setText(
                            R.string.dashboard_error_carga
                        )
                    }
                )
            }

        }.start()
    }

    private fun mostrarPerfil(perfil: FichaPerfil) {

        val objetivoTexto = when (perfil.objetivo) {

            "BAJAR" -> getString(
                R.string.dashboard_objetivo_bajar
            )

            "MANTENER" -> getString(
                R.string.dashboard_objetivo_mantener
            )

            "SUBIR" -> getString(
                R.string.dashboard_objetivo_subir
            )

            else -> perfil.objetivo
        }

        findViewById<TextView>(
            R.id.tvPerfilObjetivo
        ).text = getString(
            R.string.perfil_objetivo_formato,
            objetivoTexto
        )

        findViewById<TextView>(
            R.id.tvPerfilFecha
        ).text = getString(
            R.string.perfil_fecha_formato,
            perfil.fechaNacimiento
        )

        val edad = calcularEdad(
            perfil.fechaNacimiento
        )

        findViewById<TextView>(
            R.id.tvPerfilEdad
        ).text = if (edad != null) {
            getString(
                R.string.perfil_edad_formato,
                edad
            )
        } else {
            getString(
                R.string.perfil_edad_no_disponible
            )
        }

        findViewById<TextView>(
            R.id.tvPerfilPeso
        ).text = getString(
            R.string.perfil_peso_formato,
            perfil.peso
        )

        findViewById<TextView>(
            R.id.tvPerfilActividad
        ).text = getString(
            R.string.perfil_actividad_formato,
            perfil.actividad,
            perfil.factor
        )
    }

    private fun calcularEdad(
        fechaNacimiento: String
    ): Int? {

        val partes = fechaNacimiento
            .split("-")
            .mapNotNull { it.toIntOrNull() }

        if (partes.size != 3) {
            return null
        }

        val anio = partes[0]
        val mes = partes[1]
        val dia = partes[2]

        val nacimiento = Calendar.getInstance().apply {
            isLenient = false
            clear()
            set(anio, mes - 1, dia)
        }

        return try {

            nacimiento.timeInMillis

            val hoy = Calendar.getInstance()

            var edad = hoy.get(Calendar.YEAR) - anio

            val mesActual =
                hoy.get(Calendar.MONTH) + 1

            val diaActual =
                hoy.get(Calendar.DAY_OF_MONTH)

            if (
                mesActual < mes ||
                (mesActual == mes && diaActual < dia)
            ) {
                edad--
            }

            edad.takeIf { it in 0..120 }

        } catch (e: IllegalArgumentException) {
            null
        }
    }


    private fun mostrarSelectorObjetivo() {

        val opciones = arrayOf(
            getString(R.string.dashboard_objetivo_bajar),
            getString(R.string.dashboard_objetivo_mantener),
            getString(R.string.dashboard_objetivo_subir)
        )

        val codigos = arrayOf(
            "BAJAR",
            "MANTENER",
            "SUBIR"
        )

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.perfil_editar_objetivo)
            .setItems(opciones) { _, posicion ->

                actualizarObjetivo(codigos[posicion])
            }
            .setNegativeButton(
                R.string.perfil_cancelar,
                null
            )
            .show()
    }

    private fun actualizarObjetivo(nuevoObjetivo: String) {
        guardarCambio(
            CambioPerfil.Objetivo(nuevoObjetivo)
        )
    }


    private fun guardarCambio(cambio: CambioPerfil) {

        if (guardandoCambios) return

        val usuario = FirebaseAuth.getInstance().currentUser
            ?: return

        guardandoCambios = true

        val tvEstado = findViewById<TextView>(
            R.id.tvEstadoPerfil
        )

        tvEstado.visibility = View.VISIBLE
        tvEstado.setText(R.string.perfil_guardando_cambios)

        ActualizacionPerfilService.actualizar(
            context = applicationContext,
            firebaseUid = usuario.uid,
            cambio = cambio
        ) { resultado ->

            if (isFinishing || isDestroyed) {
                return@actualizar
            }

            guardandoCambios = false

            resultado.fold(
                onSuccess = {

                    Toast.makeText(
                        this,
                        R.string.perfil_actualizado,
                        Toast.LENGTH_SHORT
                    ).show()

                    cargarPerfil(usuario.uid)
                },

                onFailure = {

                    tvEstado.setText(
                        R.string.perfil_error_actualizar
                    )

                    Toast.makeText(
                        this,
                        R.string.perfil_error_actualizar,
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }


}