package com.sensitouch.ux.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

private val Context.almacen: DataStore<Preferences> by preferencesDataStore(name = "sensitouch")

/**
 * Todo lo que la app guarda en el celular: la cuenta del negocio, la IP del ESP32,
 * las reglas de alertas, el historial de alertas y los totales de cada dia.
 */
class Preferencias(context: Context) {

    private val almacen = context.applicationContext.almacen

    private object Llave {
        val NOMBRE = stringPreferencesKey("negocio_nombre")
        val CORREO = stringPreferencesKey("negocio_correo")
        val CLAVE = stringPreferencesKey("negocio_clave_sha256")
        val SESION = booleanPreferencesKey("sesion_activa")

        val IP = stringPreferencesKey("esp32_ip")
        val DEMO = booleanPreferencesKey("modo_demo")
        val VINCULADO = booleanPreferencesKey("vinculado")

        val NOTIFICACIONES = booleanPreferencesKey("alerta_notificaciones")
        val UMBRAL = intPreferencesKey("alerta_umbral")
        val MINIMO = intPreferencesKey("alerta_minimo_votos")
        val RACHA = booleanPreferencesKey("alerta_racha")
        val RACHA_CANTIDAD = intPreferencesKey("alerta_racha_cantidad")
        val RACHA_MINUTOS = intPreferencesKey("alerta_racha_minutos")
        val DESCONEXION = booleanPreferencesKey("alerta_desconexion")
        val RESUMEN = booleanPreferencesKey("resumen_diario")
        val HORA_RESUMEN = intPreferencesKey("resumen_hora")

        val ALERTAS = stringPreferencesKey("alertas_json")
        val AVISADAS = stringSetPreferencesKey("alertas_avisadas")
        val HISTORIAL = stringPreferencesKey("historial_json")
        val ULTIMO_CONTACTO = longPreferencesKey("ultimo_contacto")
        val ULTIMA_VISITA = longPreferencesKey("ultima_visita")
    }

    // ---------------- Cuenta ----------------

    val negocio: Flow<Negocio?> = almacen.data.map { p ->
        val correo = p[Llave.CORREO] ?: return@map null
        Negocio(p[Llave.NOMBRE] ?: "Mi negocio", correo)
    }.distinctUntilChanged()

    val sesionActiva: Flow<Boolean> = almacen.data.map { it[Llave.SESION] ?: false }.distinctUntilChanged()

    suspend fun registrar(nombre: String, correo: String, clave: String) {
        almacen.edit {
            it[Llave.NOMBRE] = nombre.trim()
            it[Llave.CORREO] = correo.trim().lowercase()
            it[Llave.CLAVE] = sha256(clave)
            it[Llave.SESION] = true
        }
    }

    /** Devuelve null si todo esta bien, o el mensaje de error para mostrar. */
    suspend fun iniciarSesion(correo: String, clave: String): String? {
        val datos = almacen.data.first()
        val guardado = datos[Llave.CORREO] ?: return "Aún no hay una cuenta en este celular. Crea la cuenta de tu negocio."
        if (guardado != correo.trim().lowercase()) return "Ese correo no está registrado en este celular."
        if (datos[Llave.CLAVE] != sha256(clave)) return "La contraseña no es correcta."
        almacen.edit { it[Llave.SESION] = true }
        return null
    }

    suspend fun cerrarSesion() {
        almacen.edit { it[Llave.SESION] = false }
    }

    // ---------------- Dispositivo ----------------

    val dispositivo: Flow<ConfigDispositivo> = almacen.data.map {
        ConfigDispositivo(
            ip = it[Llave.IP] ?: "",
            demo = it[Llave.DEMO] ?: false,
            vinculado = it[Llave.VINCULADO] ?: false,
        )
    }.distinctUntilChanged()

    suspend fun vincular(ip: String, demo: Boolean) {
        val antes = dispositivo.first()
        almacen.edit {
            it[Llave.IP] = limpiarIp(ip)
            it[Llave.DEMO] = demo
            it[Llave.VINCULADO] = true
            it[Llave.ULTIMO_CONTACTO] = System.currentTimeMillis()
            // si se pasa de la demo al ESP32 real, se borran los datos inventados
            if (antes.demo != demo) {
                it.remove(Llave.HISTORIAL)
                it.remove(Llave.ALERTAS)
                it.remove(Llave.AVISADAS)
            }
            if (demo) {
                it[Llave.HISTORIAL] = historialAJson(DemoDispositivo.historialSimulado(30))
            }
        }
    }

    suspend fun desvincular() {
        almacen.edit { it[Llave.VINCULADO] = false }
    }

    suspend fun marcarContacto() {
        almacen.edit { it[Llave.ULTIMO_CONTACTO] = System.currentTimeMillis() }
    }

    suspend fun ultimoContacto(): Long = almacen.data.first()[Llave.ULTIMO_CONTACTO] ?: 0L

    /** Guarda cuando el gerente abrio la app y devuelve la visita anterior. */
    suspend fun registrarVisita(): Long {
        val anterior = almacen.data.first()[Llave.ULTIMA_VISITA] ?: 0L
        almacen.edit { it[Llave.ULTIMA_VISITA] = System.currentTimeMillis() }
        return anterior
    }

    // ---------------- Reglas de alertas ----------------

    val configAlertas: Flow<ConfigAlertas> = almacen.data.map {
        val base = ConfigAlertas()
        ConfigAlertas(
            notificaciones = it[Llave.NOTIFICACIONES] ?: base.notificaciones,
            umbralPorcentaje = it[Llave.UMBRAL] ?: base.umbralPorcentaje,
            minimoVotos = it[Llave.MINIMO] ?: base.minimoVotos,
            rachaActiva = it[Llave.RACHA] ?: base.rachaActiva,
            rachaCantidad = it[Llave.RACHA_CANTIDAD] ?: base.rachaCantidad,
            rachaMinutos = it[Llave.RACHA_MINUTOS] ?: base.rachaMinutos,
            avisoDesconexion = it[Llave.DESCONEXION] ?: base.avisoDesconexion,
            resumenDiario = it[Llave.RESUMEN] ?: base.resumenDiario,
            horaResumen = it[Llave.HORA_RESUMEN] ?: base.horaResumen,
        )
    }.distinctUntilChanged()

    suspend fun guardarConfigAlertas(c: ConfigAlertas) {
        almacen.edit {
            it[Llave.NOTIFICACIONES] = c.notificaciones
            it[Llave.UMBRAL] = c.umbralPorcentaje
            it[Llave.MINIMO] = c.minimoVotos
            it[Llave.RACHA] = c.rachaActiva
            it[Llave.RACHA_CANTIDAD] = c.rachaCantidad
            it[Llave.RACHA_MINUTOS] = c.rachaMinutos
            it[Llave.DESCONEXION] = c.avisoDesconexion
            it[Llave.RESUMEN] = c.resumenDiario
            it[Llave.HORA_RESUMEN] = c.horaResumen
        }
    }

    // ---------------- Historial de alertas ----------------

    val alertas: Flow<List<Alerta>> = almacen.data.map { alertasDesdeJson(it[Llave.ALERTAS]) }.distinctUntilChanged()

    /**
     * Guarda solo las alertas que todavia no se habian avisado y las devuelve.
     * Asi la misma alerta no llega dos veces aunque se revise cada pocos segundos.
     */
    suspend fun agregarAlertas(candidatas: List<Alerta>): List<Alerta> {
        if (candidatas.isEmpty()) return emptyList()
        var nuevas = emptyList<Alerta>()
        almacen.edit { p ->
            val avisadas = p[Llave.AVISADAS] ?: emptySet()
            nuevas = candidatas.filter { it.id !in avisadas }
            if (nuevas.isNotEmpty()) {
                val lista = (nuevas + alertasDesdeJson(p[Llave.ALERTAS])).take(MAX_ALERTAS)
                p[Llave.ALERTAS] = alertasAJson(lista)
                p[Llave.AVISADAS] = (avisadas + nuevas.map { it.id }).toList().takeLast(200).toSet()
            }
        }
        return nuevas
    }

    suspend fun borrarAlertas() {
        almacen.edit { it.remove(Llave.ALERTAS) }
    }

    // ---------------- Totales por dia ----------------

    val historial: Flow<List<ResumenDia>> = almacen.data.map { historialDesdeJson(it[Llave.HISTORIAL]) }.distinctUntilChanged()

    suspend fun guardarResumen(dia: ResumenDia) {
        almacen.edit { p ->
            val lista = historialDesdeJson(p[Llave.HISTORIAL]).filter { it.fecha != dia.fecha } + dia
            p[Llave.HISTORIAL] = historialAJson(lista.sortedByDescending { it.fecha }.take(MAX_DIAS))
        }
    }

    // ---------------- Apoyo ----------------

    private fun sha256(texto: String): String =
        MessageDigest.getInstance("SHA-256").digest(texto.toByteArray())
            .joinToString("") { "%02x".format(it) }

    private fun alertasAJson(lista: List<Alerta>): String = JSONArray().apply {
        lista.forEach { a ->
            put(JSONObject().apply {
                put("id", a.id)
                put("tipo", a.tipo.name)
                put("titulo", a.titulo)
                put("detalle", a.detalle)
                put("fechaHora", a.fechaHora)
            })
        }
    }.toString()

    private fun alertasDesdeJson(texto: String?): List<Alerta> {
        if (texto.isNullOrBlank()) return emptyList()
        val arreglo = JSONArray(texto)
        return (0 until arreglo.length()).mapNotNull { i ->
            val o = arreglo.optJSONObject(i) ?: return@mapNotNull null
            Alerta(
                id = o.optString("id"),
                tipo = runCatching { TipoAlerta.valueOf(o.optString("tipo")) }.getOrDefault(TipoAlerta.PORCENTAJE),
                titulo = o.optString("titulo"),
                detalle = o.optString("detalle"),
                fechaHora = o.optLong("fechaHora"),
            )
        }
    }

    private fun historialAJson(lista: List<ResumenDia>): String = JSONArray().apply {
        lista.forEach { d ->
            put(JSONObject().apply {
                put("fecha", d.fecha)
                put("malo", d.malo)
                put("regular", d.regular)
                put("bueno", d.bueno)
            })
        }
    }.toString()

    private fun historialDesdeJson(texto: String?): List<ResumenDia> {
        if (texto.isNullOrBlank()) return emptyList()
        val arreglo = JSONArray(texto)
        return (0 until arreglo.length()).mapNotNull { i ->
            val o = arreglo.optJSONObject(i) ?: return@mapNotNull null
            ResumenDia(o.optString("fecha"), o.optInt("malo"), o.optInt("regular"), o.optInt("bueno"))
        }
    }

    companion object {
        private const val MAX_ALERTAS = 50
        private const val MAX_DIAS = 62
    }
}
