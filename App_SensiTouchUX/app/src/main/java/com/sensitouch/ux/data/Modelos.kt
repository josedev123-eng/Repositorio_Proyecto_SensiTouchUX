package com.sensitouch.ux.data

import kotlin.math.roundToInt

/** Las tres opiniones del dispositivo. El codigo es el mismo que usa el ESP32 (1, 2, 3). */
enum class Opinion(val codigo: Int, val etiqueta: String) {
    MALO(1, "Malo"),
    REGULAR(2, "Regular"),
    BUENO(3, "Bueno");

    companion object {
        fun desdeCodigo(codigo: Int): Opinion = entries.firstOrNull { it.codigo == codigo } ?: REGULAR
    }
}

/** Un voto que mando el ESP32. [epoch] esta en segundos (0 si el ESP32 no tenia hora de internet). */
data class Voto(
    val id: Int,
    val opinion: Opinion,
    val hora: String,
    val porBoton: Boolean,
    val epoch: Long,
)

/** Votos de una hora del dia (0..23). */
data class VotosHora(val hora: Int, val malo: Int, val regular: Int, val bueno: Int) {
    val total: Int get() = malo + regular + bueno
}

/** Todo lo que responde el ESP32 en /estado. */
data class EstadoDispositivo(
    val malo: Int = 0,
    val regular: Int = 0,
    val bueno: Int = 0,
    val accesible: Int = 0,
    val modoAccesible: Boolean = true,
    val voz: Boolean = true,
    val led: Boolean = true,
    val mensaje: String = "Selecciona tu opinion",
    val hora: String = "",
    val fecha: String = "",
    val rssi: Int = 0,
    val ip: String = "",
    val dfplayer: Boolean = false,
    val oled: Boolean = true,
    val porHora: List<VotosHora> = emptyList(),
    val ultimos: List<Voto> = emptyList(),
) {
    val total: Int get() = malo + regular + bueno

    fun cantidad(opinion: Opinion): Int = when (opinion) {
        Opinion.MALO -> malo
        Opinion.REGULAR -> regular
        Opinion.BUENO -> bueno
    }

    fun porcentaje(opinion: Opinion): Int =
        if (total == 0) 0 else (cantidad(opinion) * 100f / total).roundToInt()

    /** Indice de satisfaccion de 0 a 100: Bueno vale 100, Regular 50 y Malo 0. */
    val satisfaccion: Int
        get() = if (total == 0) 0 else ((bueno * 100f + regular * 50f) / total).roundToInt()

    /** Hora (0..23) con mas votos "Malo", o null si no hay quejas. */
    val horaConMasQuejas: Int?
        get() = porHora.filter { it.malo > 0 }.maxByOrNull { it.malo }?.hora
}

enum class TipoAlerta { PORCENTAJE, RACHA, DESCONECTADO, PRUEBA }

data class Alerta(
    val id: String,
    val tipo: TipoAlerta,
    val titulo: String,
    val detalle: String,
    val fechaHora: Long,
)

enum class EstadoConexion { CONECTANDO, CONECTADO, SIN_CONEXION, DEMO }

/** Reglas que eligio el gerente para recibir alertas. */
data class ConfigAlertas(
    val notificaciones: Boolean = true,
    val umbralPorcentaje: Int = 30,
    val minimoVotos: Int = 5,
    val rachaActiva: Boolean = true,
    val rachaCantidad: Int = 3,
    val rachaMinutos: Int = 10,
    val avisoDesconexion: Boolean = true,
    val resumenDiario: Boolean = true,
    val horaResumen: Int = 21,
)

data class ConfigDispositivo(
    val ip: String = "",
    val demo: Boolean = false,
    val vinculado: Boolean = false,
)

data class Negocio(val nombre: String, val correo: String)

/** Totales de un dia, se guardan en el celular para los reportes de semana y mes. */
data class ResumenDia(val fecha: String, val malo: Int, val regular: Int, val bueno: Int) {
    val total: Int get() = malo + regular + bueno
}

/** Deja la IP limpia aunque el usuario escriba "http://192.168.1.50/". */
fun limpiarIp(texto: String): String =
    texto.trim()
        .removePrefix("http://")
        .removePrefix("https://")
        .trimEnd('/')
