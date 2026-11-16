package cl.peaje.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class ConexionWifiTest {
    @Test fun transmiteEventosYRechazaOrdenes() {
        val listos = CountDownLatch(2)
        val recibido = CountDownLatch(1)
        val valor = AtomicReference("")
        val servidor = ConexionWifi({ if (it.contains("conectado para")) listos.countDown() }, { valor.set(it); recibido.countDown() })
        val cliente = ConexionWifi({ if (it.contains("conectado para")) listos.countDown() }, {})
        try {
            servidor.esperar("clave-compartida-larga-de-prueba")
            Thread.sleep(400)
            cliente.conectar("127.0.0.1", "clave-compartida-larga-de-prueba")
            assertTrue("Los extremos no se conectaron", listos.await(20, TimeUnit.SECONDS))
            assertFalse(cliente.enviarEvento("ORDEN|ABRIR"))
            assertTrue(cliente.enviarEvento("EVENTO|A1B2|Autorizado"))
            assertTrue("El evento no llegó", recibido.await(5, TimeUnit.SECONDS))
            assertEquals("EVENTO|A1B2|Autorizado", valor.get())
        } finally { cliente.cerrar(); servidor.cerrar() }
    }

    @Test fun rechazaClaveIncorrecta() {
        val finalizado = CountDownLatch(1)
        val servidor = ConexionWifi({ if (it.contains("clave incorrecta")) finalizado.countDown() }, {})
        val cliente = ConexionWifi({}, {})
        try {
            servidor.esperar("clave-correcta-para-prueba")
            Thread.sleep(400)
            cliente.conectar("127.0.0.1", "clave-distinta-para-prueba")
            assertTrue("La autenticación no terminó", finalizado.await(20, TimeUnit.SECONDS))
            assertFalse(cliente.enviarEvento("EVENTO|A1B2|Autorizado"))
        } finally { cliente.cerrar(); servidor.cerrar() }
    }
}
