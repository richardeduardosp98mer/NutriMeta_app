
package com.app.nutrimeta

import android.Manifest
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import java.util.Locale
import kotlin.math.roundToInt

class PesoDialog(
    private val pesoPrevio: Double? = null,
    private val onPesoGuardado: (Double) -> Unit
) : PadreDialog() {

    override val tituloResId: Int =
        R.string.titulo_dialogo_peso

    override val layoutContenidoResId: Int =
        R.layout.activity_peso_dialog

    // Valores de peso
    private val pesoInicialDecimas =
        ((pesoPrevio ?: 70.0) * 10).roundToInt()

    private var parteEntera: Int =
        pesoInicialDecimas / 10

    private var parteDecimal: Int =
        pesoInicialDecimas % 10

    // Administrador Bluetooth
    private var bleManager: BalanzaBleManager? = null

    // Control del ciclo de vida
    private var vistaActiva = false
    private var modoBluetoothActivo = false

    // Actualizaciones de interfaz en hilo principal
    private val mainHandler = Handler(Looper.getMainLooper())

    // Solicitud de permisos Bluetooth
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val concedidos = permissions.values.all { it }

            if (
                concedidos &&
                vistaActiva &&
                modoBluetoothActivo &&
                isAdded
            ) {
                bleManager?.iniciarEscaneo()
            }
        }

    override fun configurarContenido(viewHijo: View) {

        vistaActiva = true
        modoBluetoothActivo = false

        val contexto = viewHijo.context

        bleManager = BalanzaBleManager(contexto)

        // Pestañas
        val tabKg = viewHijo.findViewById<TextView>(
            R.id.tabModoKg
        )

        val tabBluetooth = viewHijo.findViewById<TextView>(
            R.id.tabModoBluetooth
        )

        // Contenedores
        val layoutManual = viewHijo.findViewById<LinearLayout>(
            R.id.layoutModoManual
        )

        val layoutBluetooth = viewHijo.findViewById<LinearLayout>(
            R.id.layoutModoBluetooth
        )

        // Controles manuales
        val tvEntero = viewHijo.findViewById<TextView>(
            R.id.tvPesoEntero
        )

        val btnRestarEntero = viewHijo.findViewById<ImageView>(
            R.id.btnRestarPesoEntero
        )

        val btnSumarEntero = viewHijo.findViewById<ImageView>(
            R.id.btnSumarPesoEntero
        )

        val tvDecimal = viewHijo.findViewById<TextView>(
            R.id.tvPesoDecimal
        )

        val btnRestarDecimal = viewHijo.findViewById<ImageView>(
            R.id.btnRestarPesoDecimal
        )

        val btnSumarDecimal = viewHijo.findViewById<ImageView>(
            R.id.btnSumarPesoDecimal
        )

        // Bluetooth y guardar
        val tvPesoBluetoothLectura =
            viewHijo.findViewById<TextView>(
                R.id.tvPesoBluetoothLectura
            )

        val btnGuardar = viewHijo.findViewById<MaterialButton>(
            R.id.btnGuardarPeso
        )

        // Valores iniciales
        tvEntero.text = parteEntera.toString()
        tvDecimal.text = parteDecimal.toString()

        // CALLBACK DE PESO BLUETOOTH

        bleManager?.onPesoDetectado = { peso, bloqueado ->

            mainHandler.post {

                // Impedir actualizaciones después
                // de cerrar el diálogo o cambiar de modo.
                if (
                    !vistaActiva ||
                    !modoBluetoothActivo ||
                    !isAdded ||
                    dialog?.isShowing != true
                ) {
                    return@post
                }

                // Descartar mediciones inválidas.
                if (!peso.isFinite() || peso <= 0.0) {
                    return@post
                }

                val pesoFormateado = String.format(
                    Locale.getDefault(),
                    "%.1f",
                    peso
                )

                if (bloqueado) {

                    tvPesoBluetoothLectura.text =
                        contexto.getString(
                            R.string.peso_capturado,
                            pesoFormateado
                        )

                    tvPesoBluetoothLectura.setTextColor(
                        ContextCompat.getColor(
                            contexto,
                            R.color.color_boton
                        )
                    )

                    // Redondear correctamente a una décima.
                    val pesoDecimas =
                        (peso * 10).roundToInt()

                    parteEntera = pesoDecimas / 10
                    parteDecimal = pesoDecimas % 10

                    tvEntero.text = parteEntera.toString()
                    tvDecimal.text = parteDecimal.toString()

                } else {

                    tvPesoBluetoothLectura.text =
                        contexto.getString(
                            R.string.peso_midiendo,
                            pesoFormateado
                        )

                    tvPesoBluetoothLectura.setTextColor(
                        ContextCompat.getColor(
                            contexto,
                            R.color.blanco
                        )
                    )
                }
            }
        }

        // CALLBACK DE ERRORES BLUETOOTH

        bleManager?.onError = { mensaje ->

            mainHandler.post {

                if (
                    !vistaActiva ||
                    !modoBluetoothActivo ||
                    !isAdded ||
                    dialog?.isShowing != true
                ) {
                    return@post
                }

                tvPesoBluetoothLectura.text = mensaje
            }
        }

        // MODO MANUAL

        tabKg.setOnClickListener {

            modoBluetoothActivo = false

            // Dejar de buscar la balanza.
            bleManager?.detenerEscaneo()

            tabKg.setBackgroundResource(
                R.drawable.fondo_toggle_activo
            )

            tabKg.setTextColor(
                ContextCompat.getColor(
                    contexto,
                    R.color.color_boton
                )
            )

            tabBluetooth.setBackgroundResource(
                android.R.color.transparent
            )

            tabBluetooth.setTextColor(
                ContextCompat.getColor(
                    contexto,
                    R.color.color_texto_toggle_inactivo
                )
            )

            layoutManual.visibility = View.VISIBLE
            layoutBluetooth.visibility = View.GONE
        }

        // MODO BLUETOOTH

        tabBluetooth.setOnClickListener {

            modoBluetoothActivo = true

            tabBluetooth.setBackgroundResource(
                R.drawable.fondo_toggle_activo
            )

            tabBluetooth.setTextColor(
                ContextCompat.getColor(
                    contexto,
                    R.color.color_boton
                )
            )

            tabKg.setBackgroundResource(
                android.R.color.transparent
            )

            tabKg.setTextColor(
                ContextCompat.getColor(
                    contexto,
                    R.color.color_texto_toggle_inactivo
                )
            )

            layoutManual.visibility = View.GONE
            layoutBluetooth.visibility = View.VISIBLE

            tvPesoBluetoothLectura.setText(
                R.string.estado_balanza_buscando
            )

            verificarPermisosYComenzar()
        }

        // CONTROLES MANUALES

        btnRestarEntero.setOnClickListener {

            if (parteEntera > 30) {
                parteEntera--
                tvEntero.text = parteEntera.toString()
            }
        }

        btnSumarEntero.setOnClickListener {

            if (parteEntera < 250) {
                parteEntera++
                tvEntero.text = parteEntera.toString()
            }
        }

        btnRestarDecimal.setOnClickListener {

            parteDecimal =
                if (parteDecimal > 0) {
                    parteDecimal - 1
                } else {
                    9
                }

            tvDecimal.text = parteDecimal.toString()
        }

        btnSumarDecimal.setOnClickListener {

            parteDecimal =
                if (parteDecimal < 9) {
                    parteDecimal + 1
                } else {
                    0
                }

            tvDecimal.text = parteDecimal.toString()
        }

        // GUARDAR EL PESO

        btnGuardar.setOnClickListener {

            if (!vistaActiva) {
                return@setOnClickListener
            }

            val pesoFinal =
                parteEntera + (parteDecimal / 10.0)

            // IMPORTANTE:
            // Desactivar callbacks ANTES de cerrar.
            liberarBluetooth()

            onPesoGuardado(pesoFinal)

            dismiss()
        }
    }

    // PERMISOS BLUETOOTH

    private fun verificarPermisosYComenzar() {

        if (!vistaActiva || !isAdded) return

        val contexto = context ?: return

        val permisos = if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        ) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }

        val faltanPermisos = permisos.any {
            ContextCompat.checkSelfPermission(
                contexto,
                it
            ) != PackageManager.PERMISSION_GRANTED
        }

        if (faltanPermisos) {

            requestPermissionLauncher.launch(permisos)

        } else {

            bleManager?.iniciarEscaneo()
        }
    }

    // LIBERAR RECURSOS BLUETOOTH

    private fun liberarBluetooth() {

        // Marcar el diálogo como inactivo.
        vistaActiva = false
        modoBluetoothActivo = false

        // Cancelar actualizaciones pendientes
        // en el hilo principal.
        mainHandler.removeCallbacksAndMessages(null)

        // Desconectar los callbacks del diálogo.
        bleManager?.onPesoDetectado = null
        bleManager?.onError = null

        // Solicitar detener el escaneo.
        bleManager?.detenerEscaneo()

        bleManager = null
    }

    override fun onDismiss(dialog: DialogInterface) {

        liberarBluetooth()

        super.onDismiss(dialog)
    }

    override fun onDestroyView() {
        liberarBluetooth()
        super.onDestroyView()
    }
}
