package com.sensitouch.ux.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Conexion con el ESP32 por WiFi (HTTP en la red local).
 *
 *  GET /estado     -> JSON con votos, votos por hora, ultimos votos y configuracion
 *  GET /config?... -> cambia voz, LED, modo accesible y mensaje de la OLED
 *  GET /reiniciar  -> pone los votos en cero
 */
class Esp32Api {

    suspend fun obtenerEstado(ip: String): EstadoDispositivo = withContext(Dispatchers.IO) {
        parsearEstado(JSONObject(pedir("http://${limpiarIp(ip)}/estado")))
    }

    suspend fun enviarConfig(
        ip: String,
        voz: Boolean? = null,
        led: Boolean? = null,
        accesible: Boolean? = null,
        mensaje: String? = null,
    ) = withContext(Dispatchers.IO) {
        val partes = buildList {
            voz?.let { add("voz=${if (it) 1 else 0}") }
            led?.let { add("led=${if (it) 1 else 0}") }
            accesible?.let { add("accesible=${if (it) 1 else 0}") }
            mensaje?.let { add("mensaje=${URLEncoder.encode(it, "UTF-8")}") }
        }
        pedir("http://${limpiarIp(ip)}/config?${partes.joinToString("&")}")
        Unit
    }

    suspend fun reiniciar(ip: String) = withContext(Dispatchers.IO) {
        pedir("http://${limpiarIp(ip)}/reiniciar")
        Unit
    }

    private fun pedir(direccion: String): String {
        val conexion = URL(direccion).openConnection() as HttpURLConnection
        conexion.connectTimeout = TIEMPO_ESPERA
        conexion.readTimeout = TIEMPO_ESPERA
        try {
            val codigo = conexion.responseCode
            if (codigo !in 200..299) throw IOException("El ESP32 respondio $codigo")
            return conexion.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conexion.disconnect()
        }
    }

    companion object {
        private const val TIEMPO_ESPERA = 3000

        fun parsearEstado(json: JSONObject): EstadoDispositivo {
            val porHora = mutableListOf<VotosHora>()
            json.optJSONArray("porHora")?.let { horas ->
                for (h in 0 until horas.length()) {
                    val fila: JSONArray = horas.optJSONArray(h) ?: continue
                    porHora += VotosHora(h, fila.optInt(0), fila.optInt(1), fila.optInt(2))
                }
            }
            val ultimos = mutableListOf<Voto>()
            json.optJSONArray("ultimos")?.let { lista ->
                for (i in 0 until lista.length()) {
                    val v = lista.optJSONObject(i) ?: continue
                    ultimos += Voto(
                        id = v.optInt("id"),
                        opinion = Opinion.desdeCodigo(v.optInt("op")),
                        hora = v.optString("hora"),
                        porBoton = v.optBoolean("boton"),
                        epoch = v.optLong("t"),
                    )
                }
            }
            return EstadoDispositivo(
                malo = json.optInt("malo"),
                regular = json.optInt("regular"),
                bueno = json.optInt("bueno"),
                accesible = json.optInt("accesible"),
                modoAccesible = json.optBoolean("modoAccesible", true),
                voz = json.optBoolean("voz", true),
                led = json.optBoolean("led", true),
                mensaje = json.optString("mensaje", "Selecciona tu opinion"),
                hora = json.optString("hora"),
                fecha = json.optString("fecha"),
                rssi = json.optInt("rssi"),
                ip = json.optString("ip"),
                dfplayer = json.optBoolean("dfplayer"),
                oled = json.optBoolean("oled", true),
                porHora = porHora,
                // el mas reciente primero
                ultimos = ultimos.sortedByDescending { it.id },
            )
        }
    }
}
