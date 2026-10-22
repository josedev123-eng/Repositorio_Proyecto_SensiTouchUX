package com.sensitouch.ux.data

import com.sensitouch.ux.alertas.EvaluadorAlertas
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Punto unico para leer el dispositivo (real o demo) y convertir sus datos en alertas.
 * Lo usan la pantalla (cada 3 s) y el trabajo en segundo plano (cada 15 min).
 */
class Repositorio(
    private val preferencias: Preferencias,
    private val api: Esp32Api = Esp32Api(),
    val demo: DemoDispositivo = DemoDispositivo(),
) {

    suspend fun leerEstado(dispositivo: ConfigDispositivo): EstadoDispositivo =
        if (dispositivo.demo) demo.estado() else api.obtenerEstado(dispositivo.ip)

    suspend fun configurar(
        dispositivo: ConfigDispositivo,
        voz: Boolean? = null,
        led: Boolean? = null,
        accesible: Boolean? = null,
        mensaje: String? = null,
    ) {
        if (dispositivo.demo) demo.configurar(voz, led, accesible, mensaje)
        else api.enviarConfig(dispositivo.ip, voz, led, accesible, mensaje)
    }

    suspend fun reiniciar(dispositivo: ConfigDispositivo) {
        if (dispositivo.demo) demo.reiniciar() else api.reiniciar(dispositivo.ip)
    }

    /**
     * Guarda los totales del dia, revisa las reglas y devuelve solo las alertas nuevas
     * (las que todavia no se avisaron).
     */
    suspend fun procesar(estado: EstadoDispositivo): List<Alerta> {
        preferencias.marcarContacto()
        val fecha = estado.fecha.ifBlank { LocalDate.now().toString() }
        preferencias.guardarResumen(ResumenDia(fecha, estado.malo, estado.regular, estado.bueno))
        val config = preferencias.configAlertas.first()
        return preferencias.agregarAlertas(EvaluadorAlertas.evaluar(estado, config))
    }

    /**
     * Se llama cuando el ESP32 no responde. Si lleva mas de [minutos] sin contestar
     * se crea una alerta (una por hora como maximo).
     */
    suspend fun revisarDesconexion(minutos: Int = 30): List<Alerta> {
        val config = preferencias.configAlertas.first()
        if (!config.avisoDesconexion) return emptyList()
        val ultimo = preferencias.ultimoContacto()
        val ahora = System.currentTimeMillis()
        if (ultimo == 0L || ahora - ultimo < minutos * 60_000L) return emptyList()
        val hora = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ultimo))
        val alerta = Alerta(
            id = "off-${LocalDate.now()}-${SimpleDateFormat("HH", Locale.getDefault()).format(Date(ahora))}",
            tipo = TipoAlerta.DESCONECTADO,
            titulo = "El SensiTouch no responde",
            detalle = "No hay datos desde las $hora. Revisa que esté encendido y conectado al WiFi; " +
                "mientras tanto no se están contando las opiniones.",
            fechaHora = ahora,
        )
        return preferencias.agregarAlertas(listOf(alerta))
    }
}
