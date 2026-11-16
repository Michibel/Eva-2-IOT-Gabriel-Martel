package cl.peaje.app

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import java.util.UUID

@SuppressLint("MissingPermission")
class ConexionBluetooth(private val adapter: BluetoothAdapter, private val estado: (String) -> Unit, private val recibir: (String) -> Unit) {
    private val uuid = UUID.fromString("7e873e20-3c3a-4c68-978c-a05fe8cb2992")
    private val uuidSerie = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
    @Volatile private var servidor: BluetoothServerSocket? = null
    @Volatile private var socket: BluetoothSocket? = null
    @Volatile private var generacion = 0
    @Volatile private var soloRecepcion = false

    fun esperar() {
        cerrar(); soloRecepcion = false; val actual = generacion
        Thread {
            try {
                val s = adapter.listenUsingRfcommWithServiceRecord("RegistroPeaje", uuid)
                servidor = s; estado("Esperando conexión de un teléfono emparejado")
                val conexion = s.accept(); s.close(); servidor = null
                if (actual == generacion) atender(conexion, actual) else conexion.close()
            } catch (e: Exception) { if (actual == generacion) estado("Conexión finalizada: ${e.message ?: "error"}") }
        }.start()
    }
    fun conectar(direccion: String, dispositivoSerie: Boolean = false) {
        cerrar(); soloRecepcion = dispositivoSerie; val actual = generacion
        Thread {
            try {
                val dispositivo = adapter.bondedDevices.firstOrNull { it.address == direccion } ?: error("Dispositivo no emparejado")
                val conexion = dispositivo.createRfcommSocketToServiceRecord(if (dispositivoSerie) uuidSerie else uuid)
                socket = conexion; estado("Conectando…"); conexion.connect()
                if (actual == generacion) atender(conexion, actual) else conexion.close()
            } catch (e: Exception) { if (actual == generacion) { cerrar(); estado("No se pudo conectar: ${e.message ?: "error"}") } }
        }.start()
    }
    private fun atender(conexion: BluetoothSocket, actual: Int) {
        socket = conexion; estado("Conectado: ${conexion.remoteDevice.name ?: "dispositivo"}")
        try {
            val entrada = conexion.inputStream
            val linea = StringBuilder()
            while (actual == generacion) {
                val b = entrada.read(); if (b == -1) break
                if (b == 10) { recibir(linea.toString()); linea.clear() }
                else if (b in 32..126 && linea.length < 160) linea.append(b.toChar())
                else throw IllegalArgumentException("Mensaje inválido")
            }
        } catch (_: Exception) {} finally {
            try { conexion.close() } catch (_: Exception) {}
            if (actual == generacion) { socket = null; estado("Desconectado") }
        }
    }
    @Synchronized fun enviar(mensaje: String): Boolean {
        if (soloRecepcion || !mensaje.matches(Regex("EVENTO\\|[A-Za-z0-9_-]{1,32}\\|(Autorizado|Denegado)"))) return false
        return try {
            val salida = socket?.outputStream ?: return false
            salida.write((mensaje + "\n").toByteArray(Charsets.UTF_8)); salida.flush(); true
        } catch (_: Exception) { false }
    }
    fun cerrar() {
        generacion++
        try { servidor?.close() } catch (_: Exception) {}; servidor = null
        try { socket?.close() } catch (_: Exception) {}; socket = null
    }
}
