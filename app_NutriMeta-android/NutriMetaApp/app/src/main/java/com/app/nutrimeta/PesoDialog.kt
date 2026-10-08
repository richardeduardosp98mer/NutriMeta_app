package com.app.nutrimeta

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton

class PesoDialog(
    private val pesoPrevio: Double? = null,
    private val onPesoGuardado: (Double) -> Unit
) : PadreDialog() {

    override val tituloResId: Int = R.string.titulo_dialogo_peso
    override val layoutContenidoResId: Int = R.layout.activity_peso_dialog

    private var parteEntera: Int = pesoPrevio?.toInt() ?: 70
    private var parteDecimal: Int = pesoPrevio?.let { ((it - it.toInt()) * 10).toInt() } ?: 0

    private var bleManager: BalanzaBleManager? = null

    // Lanzador de permisos para Bluetooth
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val concedidos = permissions.entries.all { it.value }
        if (concedidos) {
            bleManager?.iniciarEscaneo()
        }
    }

    override fun configurarContenido(viewHijo: View) {
        bleManager = BalanzaBleManager(requireContext())

        val tabKg = viewHijo.findViewById<TextView>(R.id.tabModoKg)
        val tabBluetooth = viewHijo.findViewById<TextView>(R.id.tabModoBluetooth)

        val layoutManual = viewHijo.findViewById<LinearLayout>(R.id.layoutModoManual)
        val layoutBluetooth = viewHijo.findViewById<LinearLayout>(R.id.layoutModoBluetooth)

        val tvEntero = viewHijo.findViewById<TextView>(R.id.tvPesoEntero)
        val btnRestarEntero = viewHijo.findViewById<ImageView>(R.id.btnRestarPesoEntero)
        val btnSumarEntero = viewHijo.findViewById<ImageView>(R.id.btnSumarPesoEntero)

        val tvDecimal = viewHijo.findViewById<TextView>(R.id.tvPesoDecimal)
        val btnRestarDecimal = viewHijo.findViewById<ImageView>(R.id.btnRestarPesoDecimal)
        val btnSumarDecimal = viewHijo.findViewById<ImageView>(R.id.btnSumarPesoDecimal)

        val tvPesoBluetoothLectura = viewHijo.findViewById<TextView>(R.id.tvPesoBluetoothLectura)
        val btnGuardar = viewHijo.findViewById<MaterialButton>(R.id.btnGuardarPeso)

        tvEntero.text = parteEntera.toString()
        tvDecimal.text = parteDecimal.toString()

        // Callbacks de la balanza
        bleManager?.onPesoDetectado = { peso, bloqueado ->
            val pesoFormateado = String.format("%.1f", peso)
            if (bloqueado) {
                tvPesoBluetoothLectura.text = "¡Peso capturado: $pesoFormateado kg! 🔒"
                tvPesoBluetoothLectura.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_boton))

                // Actualizar las variables de guardado
                parteEntera = peso.toInt()
                parteDecimal = ((peso - parteEntera) * 10).toInt()
                tvEntero.text = parteEntera.toString()
                tvDecimal.text = parteDecimal.toString()
            } else {
                tvPesoBluetoothLectura.text = "Midiendo: $pesoFormateado kg…"
                tvPesoBluetoothLectura.setTextColor(ContextCompat.getColor(requireContext(), R.color.blanco))
            }
        }

        bleManager?.onError = { mensaje ->
            tvPesoBluetoothLectura.text = mensaje
        }

        // Modo Manual
        tabKg.setOnClickListener {
            bleManager?.detenerEscaneo()
            tabKg.setBackgroundResource(R.drawable.fondo_toggle_activo)
            tabKg.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_boton))
            tabBluetooth.setBackgroundResource(android.R.color.transparent)
            tabBluetooth.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_texto_toggle_inactivo))

            layoutManual.visibility = View.VISIBLE
            layoutBluetooth.visibility = View.GONE
        }

        // Modo Bluetooth
        tabBluetooth.setOnClickListener {
            tabBluetooth.setBackgroundResource(R.drawable.fondo_toggle_activo)
            tabBluetooth.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_boton))
            tabKg.setBackgroundResource(android.R.color.transparent)
            tabKg.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_texto_toggle_inactivo))

            layoutManual.visibility = View.GONE
            layoutBluetooth.visibility = View.VISIBLE

            tvPesoBluetoothLectura.setText(R.string.estado_balanza_buscando)
            verificarPermisosYComenzar()
        }

        // Steppers manuales
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
            parteDecimal = if (parteDecimal > 0) parteDecimal - 1 else 9
            tvDecimal.text = parteDecimal.toString()
        }
        btnSumarDecimal.setOnClickListener {
            parteDecimal = if (parteDecimal < 9) parteDecimal + 1 else 0
            tvDecimal.text = parteDecimal.toString()
        }

        btnGuardar.setOnClickListener {
            val pesoFinal = parteEntera + (parteDecimal / 10.0)
            onPesoGuardado(pesoFinal)
            dismiss()
        }
    }

    private fun verificarPermisosYComenzar() {
        val permisos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }

        if (faltanPermisos) {
            requestPermissionLauncher.launch(permisos)
        } else {
            bleManager?.iniciarEscaneo()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bleManager?.detenerEscaneo()
    }
}