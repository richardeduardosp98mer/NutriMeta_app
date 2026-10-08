package com.app.nutrimeta

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

class BalanzaBleManager(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        manager.adapter
    }

    private val bleScanner: BluetoothLeScanner?
        get() = bluetoothAdapter?.bluetoothLeScanner

    private var isScanning = false
    private val mainHandler = Handler(Looper.getMainLooper())

    var onPesoDetectado: ((peso: Double, bloqueado: Boolean) -> Unit)? = null
    var onError: ((mensaje: String) -> Unit)? = null

    fun estaBluetoothHabilitado(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    @SuppressLint("MissingPermission")
    fun iniciarEscaneo() {
        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            onError?.invoke("Enciende el Bluetooth del teléfono")
            return
        }

        if (isScanning) return

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0)
            .build()

        try {
            isScanning = true
            bleScanner?.startScan(null, settings, scanCallback)
        } catch (e: Exception) {
            isScanning = false
            onError?.invoke("Error al iniciar escaneo BLE: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    fun detenerEscaneo() {
        if (!isScanning) return
        try {
            bleScanner?.stopScan(scanCallback)
        } catch (_: Exception) { }
        isScanning = false
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result ?: return
            val bytes = result.scanRecord?.bytes ?: return
            decodificarTramaBalanza(bytes)
        }

        override fun onScanFailed(errorCode: Int) {
            mainHandler.post {
                onError?.invoke("Fallo al escanear dispositivos (Código: $errorCode)")
            }
        }
    }

    private fun decodificarTramaBalanza(bytes: ByteArray) {
        val len = bytes.size

        for (i in 0 until len) {
            val byteActual = bytes[i].toInt() and 0xFF

            if (byteActual == 0xC0 && (i + 8) < len) {
                val estado = bytes[i + 8].toInt() and 0xFF
                val bloqueado = (estado == 0x25)
                val pesoRaw = ((bytes[i + 2].toInt() and 0xFF) shl 8) or (bytes[i + 3].toInt() and 0xFF)
                val pesoKg = pesoRaw / 100.0

                if (pesoKg in 5.0..220.0) {
                    mainHandler.post { onPesoDetectado?.invoke(pesoKg, bloqueado) }
                    return
                }
            }

            if (byteActual == 0xCA && (i + 6) < len) {
                if ((bytes[i + 1].toInt() and 0xFF) == 0x11) {
                    val bloqueado = (bytes[i + 3].toInt() and 0xFF) == 0x01
                    val pesoRaw = ((bytes[i + 5].toInt() and 0xFF) shl 8) or (bytes[i + 6].toInt() and 0xFF)

                    var pesoKg = pesoRaw / 100.0
                    if (pesoKg < 5.0) {
                        pesoKg = pesoRaw / 10.0
                    }

                    if (pesoKg in 5.0..220.0) {
                        mainHandler.post { onPesoDetectado?.invoke(pesoKg, bloqueado) }
                        return
                    }
                }
            }

            if (byteActual == 0xCB && (i + 6) < len) {
                if ((bytes[i + 1].toInt() and 0xFF) == 0x10) {
                    val bloqueado = (bytes[i + 3].toInt() and 0xFF) == 0x01
                    val pesoRaw = ((bytes[i + 5].toInt() and 0xFF) shl 8) or (bytes[i + 6].toInt() and 0xFF)
                    val pesoKg = pesoRaw / 100.0

                    if (pesoKg in 5.0..220.0) {
                        mainHandler.post { onPesoDetectado?.invoke(pesoKg, bloqueado) }
                        return
                    }
                }
            }
        }
    }
}