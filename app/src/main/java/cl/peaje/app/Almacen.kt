package cl.peaje.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec

data class Paso(val fecha: Long, val uid: String, val resultado: String, val origen: String)

class Almacen(contexto: Context) : SQLiteOpenHelper(contexto, "peaje.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE usuario (nombre TEXT PRIMARY KEY, sal TEXT NOT NULL, clave TEXT NOT NULL)")
        db.execSQL("CREATE TABLE pasos (id INTEGER PRIMARY KEY AUTOINCREMENT, fecha INTEGER NOT NULL, uid TEXT NOT NULL, resultado TEXT NOT NULL, origen TEXT NOT NULL)")
    }
    override fun onUpgrade(db: SQLiteDatabase, anterior: Int, nuevo: Int) {}

    fun existeUsuario(): Boolean = readableDatabase.rawQuery("SELECT nombre FROM usuario LIMIT 1", null).use { it.moveToFirst() }
    private fun derivar(clave: String, sal: ByteArray): ByteArray {
        val spec = PBEKeySpec(clave.toCharArray(), sal, 210000, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded } finally { spec.clearPassword() }
    }
    fun crearUsuario(nombre: String, clave: String) {
        val sal = ByteArray(16); SecureRandom().nextBytes(sal)
        val valores = ContentValues().apply {
            put("nombre", nombre); put("sal", Base64.encodeToString(sal, Base64.NO_WRAP))
            put("clave", Base64.encodeToString(derivar(clave, sal), Base64.NO_WRAP))
        }
        writableDatabase.insertOrThrow("usuario", null, valores)
    }
    fun validar(nombre: String, clave: String): Boolean = readableDatabase.rawQuery("SELECT sal, clave FROM usuario WHERE nombre = ?", arrayOf(nombre)).use {
        if (!it.moveToFirst()) false else {
            val sal = Base64.decode(it.getString(0), Base64.NO_WRAP)
            val esperado = Base64.decode(it.getString(1), Base64.NO_WRAP)
            MessageDigest.isEqual(esperado, derivar(clave, sal))
        }
    }
    private fun llave(): javax.crypto.SecretKey {
        val almacen = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existente = almacen.getKey("clave_uid", null) as? javax.crypto.SecretKey
        if (existente != null) return existente
        val generador = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generador.init(KeyGenParameterSpec.Builder("clave_uid", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        return generador.generateKey()
    }
    private fun cifrar(texto: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE, llave())
        return Base64.encodeToString(cipher.iv + cipher.doFinal(texto.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
    }
    private fun descifrar(texto: String): String {
        val bytes = Base64.decode(texto, Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, llave(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        return String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8)
    }
    fun guardar(paso: Paso) {
        val valores = ContentValues().apply { put("fecha", paso.fecha); put("uid", cifrar(paso.uid)); put("resultado", paso.resultado); put("origen", paso.origen) }
        writableDatabase.insertOrThrow("pasos", null, valores)
    }
    fun listar(): List<Paso> {
        val lista = mutableListOf<Paso>()
        readableDatabase.rawQuery("SELECT fecha, uid, resultado, origen FROM pasos ORDER BY fecha DESC, id DESC", null).use {
            while (it.moveToNext()) lista.add(Paso(it.getLong(0), descifrar(it.getString(1)), it.getString(2), it.getString(3)))
        }
        return lista
    }
}
