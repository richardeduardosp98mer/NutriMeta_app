package com.app.nutrimeta.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.app.nutrimeta.R

class BalanzaBleManager(context: Context) {

    // Utilizamos el contexto de la aplicación para
    // evitar mantener referencias innecesarias a Activities.
    private val appContext = context.applicationContext

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = appContext.getSystemService(
            Context.BLUETOOTH_SERVICE
        ) as? BluetoothManager

        manager?.adapter
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    // Estado del escaneo, visible entre hilos.
    @Volatile
    private var isScanning = false

    private var scannerActivo: BluetoothLeScanner? = null

    // Callbacks hacia PesoDialog
    var onPesoDetectado: ((
        peso: Double,
        bloqueado: Boolean
    ) -> Unit)? = null

    var onError: ((mensaje: String) -> Unit)? = null

    @SuppressLint("MissingPermission")
    fun estaBluetoothHabilitado(): Boolean {
        return try {
            bluetoothAdapter?.isEnabled == true
        } catch (e: SecurityException) {
            false
        }
    }

    // INICIAR ESCANEO BLUETOOTH

    @SuppressLint("MissingPermission")
    fun iniciarEscaneo() {

        if (isScanning) return

        try {

            val adapter = bluetoothAdapter

            if (adapter == null || !adapter.isEnabled) {

                notificarError(
                    appContext.getString(
                        R.string.error_bluetooth
                    )
                )

                return
            }

            val scanner = adapter.bluetoothLeScanner

            if (scanner == null) {

                notificarError(
                    appContext.getString(
                        R.string.error_escaner_ble_no_disponible
                    )
                )

                return
            }

            val settings = ScanSettings.Builder()
                .setScanMode(
                    ScanSettings.SCAN_MODE_LOW_LATENCY
                )
                .setReportDelay(0)
                .build()

            scannerActivo = scanner
            isScanning = true

            scanner.startScan(
                null,
                settings,
                scanCallback
            )

        } catch (e: Exception) {

            isScanning = false
            scannerActivo = null

            notificarError(
                appContext.getString(
                    R.string.error_escaneo,
                    e.localizedMessage
                        ?: e.javaClass.simpleName
                )
            )
        }
    }

    // DETENER ESCANEO BLUETOOTH

    @SuppressLint("MissingPermission")
    fun detenerEscaneo() {

        // Primero invalidamos las mediciones.
        val estabaEscaneando = isScanning
        isScanning = false

        // Cancelamos las actualizaciones pendientes.
        mainHandler.removeCallbacksAndMessages(null)

        val scanner = scannerActivo
        scannerActivo = null

        if (estabaEscaneando && scanner != null) {

            try {
                scanner.stopScan(scanCallback)

            } catch (e: Exception) {
                // La detención puede fallar si el
                // Bluetooth fue desactivado.
                // El diálogo ya está protegido.
            }
        }
    }

    // CALLBACK DEL ESCÁNER

    private val scanCallback = object : ScanCallback() {

        override fun onScanResult(
            callbackType: Int,
            result: ScanResult?
        ) {

            if (!isScanning) return

            val bytes = result?.scanRecord?.bytes
                ?: return

            decodificarTramaBalanza(bytes)
        }

        override fun onScanFailed(errorCode: Int) {

            mainHandler.post {

                if (!isScanning) {
                    return@post
                }

                isScanning = false
                scannerActivo = null

                onError?.invoke(
                    appContext.getString(
                        R.string.error_escaneo,
                        errorCode.toString()
                    )
                )
            }
        }
    }

    // ENVIAR PESO AL DIÁLOGO

    private fun entregarPeso(
        pesoKg: Double,
        bloqueado: Boolean
    ) {

        if (!isScanning) return

        if (
            !pesoKg.isFinite() ||
            pesoKg !in 5.0..220.0
        ) {
            return
        }

        mainHandler.post {

            // Evitar entregar mediciones después
            // de que el usuario cierre el diálogo.
            if (!isScanning) {
                return@post
            }

            onPesoDetectado?.invoke(
                pesoKg,
                bloqueado
            )
        }
    }

    // NOTIFICAR ERRORES

    private fun notificarError(mensaje: String) {

        mainHandler.post {
            onError?.invoke(mensaje)
        }
    }

    // DECODIFICAR MEDICIONES

    private fun decodificarTramaBalanza(
        bytes: ByteArray
    ) {

        if (!isScanning) return

        val len = bytes.size

        for (i in 0 until len) {

            val byteActual =
                bytes[i].toInt() and 0xFF

            // FORMATO 1: 0xC0
            if (
                byteActual == 0xC0 &&
                (i + 8) < len
            ) {

                val estado =
                    bytes[i + 8].toInt() and 0xFF

                val bloqueado = estado == 0x25

                val pesoRaw =
                    ((bytes[i + 2].toInt() and 0xFF) shl 8) or
                            (bytes[i + 3].toInt() and 0xFF)

                val pesoKg = pesoRaw / 100.0

                if (pesoKg in 5.0..220.0) {

                    entregarPeso(
                        pesoKg,
                        bloqueado
                    )

                    return
                }
            }

            // FORMATO 2: 0xCA
            if (
                byteActual == 0xCA &&
                (i + 6) < len
            ) {

                if (
                    (bytes[i + 1].toInt() and 0xFF) == 0x11
                ) {

                    val bloqueado =
                        (bytes[i + 3].toInt() and 0xFF) == 0x01

                    val pesoRaw =
                        ((bytes[i + 5].toInt() and 0xFF) shl 8) or
                                (bytes[i + 6].toInt() and 0xFF)

                    var pesoKg = pesoRaw / 100.0

                    if (pesoKg < 5.0) {
                        pesoKg = pesoRaw / 10.0
                    }

                    if (pesoKg in 5.0..220.0) {

                        entregarPeso(
                            pesoKg,
                            bloqueado
                        )

                        return
                    }
                }
            }

            // FORMATO 3: 0xCB
            if (
                byteActual == 0xCB &&
                (i + 6) < len
            ) {

                if (
                    (bytes[i + 1].toInt() and 0xFF) == 0x10
                ) {

                    val bloqueado =
                        (bytes[i + 3].toInt() and 0xFF) == 0x01

                    val pesoRaw =
                        ((bytes[i + 5].toInt() and 0xFF) shl 8) or
                                (bytes[i + 6].toInt() and 0xFF)

                    val pesoKg = pesoRaw / 100.0

                    if (pesoKg in 5.0..220.0) {

                        entregarPeso(
                            pesoKg,
                            bloqueado
                        )

                        return
                    }
                }
            }
        }
    }
}