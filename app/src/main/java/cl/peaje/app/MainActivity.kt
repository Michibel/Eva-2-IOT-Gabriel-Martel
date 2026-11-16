package cl.peaje.app

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

class MainActivity : ComponentActivity() {
    private lateinit var almacen: Almacen
    private var conexion: ConexionBluetooth? = null
    private var wifi: ConexionWifi? = null
    private var estadoWifi by mutableStateOf("Wi-Fi: desconectado")
    private val handler = Handler(Looper.getMainLooper())
    private var sesion by mutableStateOf(false)
    private var mensaje by mutableStateOf("")
    private var estadoConexion by mutableStateOf("Desconectado")
    private var marcha by mutableStateOf(false)
    private var barrera by mutableStateOf("Desconocida (sin datos del equipo)")
    private var pasos by mutableStateOf<List<Paso>>(emptyList())
    private var dispositivos by mutableStateOf<List<Pair<String, String>>>(emptyList())
    private var exportacion: List<Paso> = emptyList()
    private val cerrarSesion = Runnable { salir(); mensaje = "Sesión cerrada por inactividad" }
    private val permisos = registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) listarDispositivos() else mensaje = "Se necesita permiso de dispositivos cercanos para Bluetooth"
    }
    private val guardarCsv = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null && sesion) {
            val copia = exportacion
            Thread {
                try {
                    contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { salida ->
                        salida.write("fecha_hora,uid,metodo,resultado,origen\n")
                        copia.forEach { p -> salida.write(listOf(fecha(p.fecha), p.uid, "RFID", p.resultado, p.origen).joinToString(",") { csv(it) } + "\n") }
                    } ?: error("No se pudo abrir el archivo")
                    runOnUiThread { mensaje = "CSV exportado" }
                } catch (_: Exception) { runOnUiThread { mensaje = "No se pudo exportar el CSV" } }
            }.start()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        almacen = Almacen(this)
        val adapter = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
        if (adapter != null) conexion = ConexionBluetooth(adapter, { texto -> runOnUiThread { estadoConexion = texto } }, { linea -> runOnUiThread { procesar(linea, "Bluetooth") } })
        wifi = ConexionWifi({ texto -> runOnUiThread { estadoWifi = texto } }, { linea -> runOnUiThread { procesar(linea, "Wi-Fi") } })
        setContent { MaterialTheme { Surface(modifier = Modifier.fillMaxSize()) { Pantalla() } } }
    }
    override fun onUserInteraction() { super.onUserInteraction(); if (sesion) reiniciarTiempo() }
    private fun reiniciarTiempo() { handler.removeCallbacks(cerrarSesion); handler.postDelayed(cerrarSesion, 5 * 60 * 1000L) }
    private fun salir() { sesion = false; handler.removeCallbacks(cerrarSesion); conexion?.cerrar(); wifi?.cerrar(); estadoConexion = "Desconectado"; estadoWifi = "Wi-Fi: desconectado"; pasos = emptyList(); marcha = false; barrera = "Desconocida (sin datos del equipo)" }
    override fun onDestroy() { handler.removeCallbacks(cerrarSesion); conexion?.cerrar(); wifi?.cerrar(); almacen.close(); super.onDestroy() }
    private fun fecha(milisegundos: Long) = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(milisegundos))
    private fun csv(valor: String): String {
        val seguro = if (valor.startsWith('=') || valor.startsWith('+') || valor.startsWith('-') || valor.startsWith('@')) "'$valor" else valor
        return "\"" + seguro.replace("\"", "\"\"") + "\""
    }
    private fun actualizar() { pasos = almacen.listar() }
    private fun registrar(uid: String, resultado: String, origen: String) {
        almacen.guardar(Paso(System.currentTimeMillis(), uid, resultado, origen)); actualizar()
    }
    private fun procesar(linea: String, origen: String) {
        if (!sesion) return
        val partes = linea.split('|')
        when {
            partes.size == 3 && partes[0] == "EVENTO" && partes[1].matches(Regex("[A-Za-z0-9_-]{1,32}")) && partes[2] in listOf("Autorizado", "Denegado") -> registrar(partes[1], partes[2], origen)
            else -> mensaje = "Mensaje de $origen no reconocido"
        }
    }
    @SuppressLint("MissingPermission")
    private fun listarDispositivos() {
        if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) { permisos.launch(Manifest.permission.BLUETOOTH_CONNECT); return }
        val adapter = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
        if (adapter == null || !adapter.isEnabled) { mensaje = "Activa Bluetooth y empareja los teléfonos en Ajustes"; return }
        try { dispositivos = adapter.bondedDevices.map { (it.name ?: "Sin nombre") to it.address }; if (dispositivos.isEmpty()) mensaje = "No hay dispositivos emparejados" }
        catch (_: SecurityException) { mensaje = "Permiso Bluetooth no disponible" }
    }
    private fun enviarEvento(uid: String, resultado: String) {
        Thread {
            val linea = "EVENTO|$uid|$resultado"
            val canal = when {
                wifi?.enviarEvento(linea) == true -> "Wi-Fi"
                conexion?.enviar(linea) == true -> "Bluetooth"
                else -> null
            }
            runOnUiThread {
                if (sesion) mensaje = if (canal == null) "Guardado localmente; sin conexión para transmitir" else "Enviado por $canal; recepción no confirmada"
            }
        }.start()
    }

    @Composable fun Pantalla() {
        var usuario by remember { mutableStateOf("") }; var clave by remember { mutableStateOf("") }
        var pestana by remember { mutableStateOf(0) }; var filtro by remember { mutableStateOf("Todos") }
        var busqueda by remember { mutableStateOf("") }; var desde by remember { mutableStateOf("") }; var hasta by remember { mutableStateOf("") }
        var ipWifi by remember { mutableStateOf("") }; var claveWifi by remember { mutableStateOf("") }
        var confirmar by remember { mutableStateOf("") }; var ocupado by remember { mutableStateOf(false) }
        val prefs = remember { getSharedPreferences("acceso", MODE_PRIVATE) }
        val primerIngreso = remember(sesion) { !almacen.existeUsuario() }
        if (!sesion) {
            Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
                Text(if (primerIngreso) "Crear usuario local" else "Registro de Peaje", style = MaterialTheme.typography.headlineMedium)
                TextField(usuario, { usuario = it }, label = { Text("Usuario") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                TextField(clave, { clave = it }, label = { Text("Contraseña") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Button(enabled = !ocupado, onClick = {
                    val u = usuario.trim(); val c = clave; val bloqueo = prefs.getLong("bloqueo", 0)
                    when {
                        System.currentTimeMillis() < bloqueo -> mensaje = "Espera un minuto antes de volver a intentar"
                        u.isBlank() || c.isBlank() -> mensaje = "Completa usuario y contraseña"
                        primerIngreso && (u.length > 40 || c.length < 8) -> mensaje = "Usa un usuario de hasta 40 caracteres y una contraseña de al menos 8"
                        else -> {
                            ocupado = true
                            Thread {
                                try {
                                    val correcto = if (primerIngreso) { almacen.crearUsuario(u, c); true } else almacen.validar(u, c)
                                    runOnUiThread {
                                        ocupado = false; clave = ""
                                        if (correcto) { prefs.edit().putInt("intentos", 0).putLong("bloqueo", 0).apply(); sesion = true; mensaje = ""; actualizar(); reiniciarTiempo() }
                                        else { val intentos = prefs.getInt("intentos", 0) + 1; val editor = prefs.edit(); if (intentos >= 3) { editor.putInt("intentos", 0).putLong("bloqueo", System.currentTimeMillis() + 60000); mensaje = "Tres intentos fallidos. Espera un minuto" } else { editor.putInt("intentos", intentos); mensaje = "Credenciales incorrectas" }; editor.apply() }
                                    }
                                } catch (_: Exception) { runOnUiThread { ocupado = false; mensaje = "No se pudo acceder al almacenamiento" } }
                            }.start()
                        }
                    }
                }) { Text(if (primerIngreso) "Crear e ingresar" else "Ingresar") }
                Text(mensaje, color = MaterialTheme.colorScheme.error)
            }
        } else {
            val formato = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply { isLenient = false } }
            fun limite(valor: String): Long? = try { if (valor.isBlank() || !valor.matches(Regex("\\d{2}/\\d{2}/\\d{4}"))) null else formato.parse(valor)?.time } catch (_: Exception) { null }
            val inicio = limite(desde); val fin = limite(hasta)
            val fechasValidas = (desde.isBlank() || inicio != null) && (hasta.isBlank() || fin != null) && (inicio == null || fin == null || inicio <= fin)
            val finExclusivo = fin?.let { Calendar.getInstance().apply { timeInMillis = it; add(Calendar.DAY_OF_MONTH, 1) }.timeInMillis }
            val filtrados = if (!fechasValidas) emptyList() else pasos.filter { it.uid.contains(busqueda.trim(), true) && (filtro == "Todos" || it.resultado == filtro) && (inicio == null || it.fecha >= inicio) && (finExclusivo == null || it.fecha < finExclusivo) }
            Scaffold(topBar = { Column(Modifier.padding(12.dp)) { Text("Registro de Peaje", style = MaterialTheme.typography.headlineSmall); Text(estadoConexion); Text(estadoWifi); Text(mensaje, color = MaterialTheme.colorScheme.primary) } }, bottomBar = {
                NavigationBar { listOf("Monitoreo", "Activador", "Historial", "Conexión").forEachIndexed { i, titulo -> NavigationBarItem(selected = pestana == i, onClick = { pestana = i }, icon = { Text("${i + 1}") }, label = { Text(titulo) }) } }
            }) { relleno ->
                LazyColumn(Modifier.fillMaxSize().padding(relleno).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (pestana) {
                        0 -> { item { Text("Monitoreo", style = MaterialTheme.typography.headlineSmall); Text("Simulación local: ${if (marcha) "Activa" else "Detenida"}"); Text("Barrera física: $barrera"); Text("Pasos de hoy: ${pasos.count { fecha(it.fecha).take(10) == fecha(System.currentTimeMillis()).take(10) }}"); Text("Último evento: ${pasos.firstOrNull()?.let { "${it.uid} - ${it.resultado} (${fecha(it.fecha)})" } ?: "Sin eventos"}"); Text("No hay conexión con la barrera física.") } }
                        1 -> { item { Text("Control simulado", style = MaterialTheme.typography.headlineSmall); Row { Text("Simulación activa"); Switch(marcha, { marcha = it }) }; Text("La simulación no envía órdenes a la barrera. Simular paso RFID:") }
                            items(listOf("Autorizado", "Denegado")) { resultado -> Button(enabled = marcha, onClick = { val uid = if (resultado == "Autorizado") "A1B2C3" else "D4E5F6"; registrar(uid, resultado, "Simulación"); enviarEvento(uid, resultado) }) { Text("Simular tarjeta $resultado") } } }
                        2 -> { item { Text("Historial", style = MaterialTheme.typography.headlineSmall); TextField(busqueda, { busqueda = it }, label = { Text("Buscar UID") }, singleLine = true); Row { listOf("Todos", "Autorizado", "Denegado").forEach { opcion -> Row { RadioButton(filtro == opcion, { filtro = opcion }); Text(opcion) } } }; TextField(desde, { desde = it }, label = { Text("Desde dd/MM/aaaa") }, singleLine = true); TextField(hasta, { hasta = it }, label = { Text("Hasta dd/MM/aaaa") }, singleLine = true); if (!fechasValidas) Text("Revisa las fechas", color = MaterialTheme.colorScheme.error); Text("Mostrados: ${filtrados.size} · Autorizados: ${filtrados.count { it.resultado == "Autorizado" }} · Denegados: ${filtrados.count { it.resultado == "Denegado" }}"); Button(onClick = { confirmar = "exportar" }, enabled = fechasValidas) { Text("Exportar CSV") } }
                            items(filtrados) { paso -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) { Text("${paso.resultado} · ${paso.uid}"); Text("${fecha(paso.fecha)} · RFID · ${paso.origen}") } } } }
                        else -> { item {
                            Text("Supervisión Wi-Fi entre teléfonos", style = MaterialTheme.typography.headlineSmall)
                            Text("Usa la misma red local. En el teléfono receptor busca su dirección IP en los ajustes de Wi-Fi. La clave compartida debe tener al menos 12 caracteres.")
                            TextField(claveWifi, { claveWifi = it }, label = { Text("Clave compartida de Wi-Fi") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                            TextField(ipWifi, { ipWifi = it }, label = { Text("IP del teléfono que espera") }, singleLine = true)
                            Button(onClick = { if (claveWifi.length < 12) mensaje = "La clave compartida debe tener al menos 12 caracteres" else wifi?.esperar(claveWifi) }) { Text("Esperar supervisión Wi-Fi") }
                            Button(onClick = { if (claveWifi.length < 12 || ipWifi.isBlank()) mensaje = "Indica la IP y una clave de al menos 12 caracteres" else wifi?.conectar(ipWifi.trim(), claveWifi) }) { Text("Conectar por Wi-Fi") }
                            OutlinedButton(onClick = { wifi?.cerrar(); estadoWifi = "Wi-Fi: desconectado" }) { Text("Desconectar Wi-Fi") }
                            Text("Solo se intercambian eventos simulados; no se envían órdenes de apertura.")
                            Text("Conexión Bluetooth", style = MaterialTheme.typography.headlineSmall); Text("Empareja ambos teléfonos desde los ajustes de Android. En uno espera y en el otro selecciona el primero."); Button(onClick = { listarDispositivos() }) { Text("Consultar emparejados y permisos") }; Button(onClick = { if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) permisos.launch(Manifest.permission.BLUETOOTH_CONNECT) else conexion?.esperar() }) { Text("Esperar conexión") }; Button(onClick = { conexion?.cerrar(); estadoConexion = "Desconectado" }) { Text("Desconectar") } }
                            items(dispositivos) { dispositivo -> Column {
                                Text("${dispositivo.first} (${dispositivo.second})")
                                TextButton(onClick = { conexion?.conectar(dispositivo.second) }) { Text("Conectar como teléfono") }
                                TextButton(onClick = { conexion?.conectar(dispositivo.second, dispositivoSerie = true); mensaje = "HC-05: solo recepción de eventos compatibles; conexión sin verificar" }) { Text("Conectar como HC-05 (solo lectura)") }
                            } }
                            item { OutlinedButton(onClick = { salir(); clave = "" }) { Text("Cerrar sesión") } } }
                    }
                }
            }
            if (confirmar.isNotEmpty()) AlertDialog(onDismissRequest = { confirmar = "" }, title = { Text("Confirmar") }, text = { Text("¿Exportar los registros mostrados?") }, confirmButton = { TextButton(onClick = { exportacion = filtrados.toList(); guardarCsv.launch("historial_peaje.csv"); confirmar = "" }) { Text("Aceptar") } }, dismissButton = { TextButton(onClick = { confirmar = "" }) { Text("Cancelar") } })
        }
    }
}
