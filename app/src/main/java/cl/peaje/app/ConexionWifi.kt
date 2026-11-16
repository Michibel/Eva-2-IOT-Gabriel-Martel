package cl.peaje.app

import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

// Canal de demostración entre dos teléfonos. Solo admite eventos de monitoreo.
class ConexionWifi(private val estado: (String) -> Unit, private val recibir: (String) -> Unit) {
    @Volatile private var servidor: ServerSocket? = null
    @Volatile private var socket: Socket? = null
    @Volatile private var generacion = 0
    private var salida: DataOutputStream? = null
    private var clave: SecretKeySpec? = null
    private var contadorSalida = 0L
    private val azar = SecureRandom()
    private val puerto = 37373

    fun esperar(secreto: String) {
        cerrar(); val actual = generacion
        Thread {
            try {
                val s = ServerSocket(puerto); servidor = s
                estado("Wi-Fi: esperando un teléfono en el puerto $puerto")
                val c = s.accept(); s.close(); servidor = null
                if (actual == generacion) atender(c, secreto, true, actual) else c.close()
            } catch (_: Exception) { if (actual == generacion) { cerrar(); estado("Wi-Fi: no se pudo recibir la conexión") } }
        }.start()
    }
    fun conectar(ip: String, secreto: String) {
        cerrar(); val actual = generacion
        Thread {
            try {
                val c = Socket(); socket = c; c.connect(InetSocketAddress(ip, puerto), 5000)
                if (actual == generacion) atender(c, secreto, false, actual) else c.close()
            } catch (_: Exception) { if (actual == generacion) { cerrar(); estado("Wi-Fi: conexión o autenticación fallida") } }
        }.start()
    }
    private fun atender(c: Socket, secreto: String, esServidor: Boolean, actual: Int) {
        socket = c; c.soTimeout = 15000
        val entrada = DataInputStream(c.getInputStream()); val salidaLocal = DataOutputStream(c.getOutputStream())
        val nonceServidor = ByteArray(16); val nonceCliente = ByteArray(16)
        try {
            if (esServidor) { azar.nextBytes(nonceServidor); salidaLocal.write(nonceServidor); salidaLocal.flush(); entrada.readFully(nonceCliente) }
            else { entrada.readFully(nonceServidor); azar.nextBytes(nonceCliente); salidaLocal.write(nonceCliente); salidaLocal.flush() }
            val spec = PBEKeySpec(secreto.toCharArray(), nonceServidor + nonceCliente, 150000, 256)
            val llave = try { SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded, "AES") } finally { spec.clearPassword() }
            // Direcciones separadas impiden reutilizar un nonce con la misma clave.
            val dirSalida = if (esServidor) 1 else 2; val dirEntrada = if (esServidor) 2 else 1
            if (esServidor) {
                if (leer(entrada, llave, dirEntrada, 0) != "HOLA") error("Autenticación fallida")
                escribir(salidaLocal, llave, dirSalida, 0, "LISTO")
            } else {
                escribir(salidaLocal, llave, dirSalida, 0, "HOLA")
                if (leer(entrada, llave, dirEntrada, 0) != "LISTO") error("Autenticación fallida")
            }
            synchronized(this) { if (actual != generacion) return; servidorActivo = esServidor; salida = salidaLocal; clave = llave; contadorSalida = 1 }
            c.soTimeout = 0; estado("Wi-Fi: conectado para supervisión")
            var contadorEntrada = 1L
            while (actual == generacion) { recibir(leer(entrada, llave, dirEntrada, contadorEntrada)); contadorEntrada++ }
        } catch (_: Exception) { if (actual == generacion) estado("Wi-Fi: desconectado o clave incorrecta") }
        finally { try { c.close() } catch (_: Exception) {}; if (actual == generacion) { synchronized(this) { salida = null; clave = null }; socket = null; estado("Wi-Fi: desconectado o clave incorrecta") } }
    }
    private fun iv(direccion: Int, numero: Long): ByteArray = java.nio.ByteBuffer.allocate(12).putInt(direccion).putLong(numero).array()
    private fun escribir(out: DataOutputStream, key: SecretKeySpec, direccion: Int, numero: Long, texto: String) {
        val c = Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv(direccion, numero)))
        val datos = c.doFinal(texto.toByteArray(Charsets.UTF_8)); out.writeInt(datos.size); out.write(datos); out.flush()
    }
    private fun leer(input: DataInputStream, key: SecretKeySpec, direccion: Int, numero: Long): String {
        val largo = input.readInt(); require(largo in 16..176)
        val datos = ByteArray(largo); input.readFully(datos)
        val c = Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv(direccion, numero)))
        return String(c.doFinal(datos), Charsets.UTF_8)
    }
    @Synchronized fun enviarEvento(texto: String): Boolean {
        if (!texto.matches(Regex("EVENTO\\|[A-Za-z0-9_-]{1,32}\\|(Autorizado|Denegado)"))) return false
        return try { val out = salida ?: return false; val key = clave ?: return false; escribir(out, key, if (servidorActivo) 1 else 2, contadorSalida++, texto); true } catch (_: Exception) { false }
    }
    @Volatile private var servidorActivo = false
    @Synchronized fun cerrar() {
        generacion++; try { servidor?.close() } catch (_: Exception) {}; servidor = null
        try { socket?.close() } catch (_: Exception) {}; socket = null; salida = null; clave = null
    }
}
