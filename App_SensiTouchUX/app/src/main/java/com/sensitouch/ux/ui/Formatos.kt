package com.sensitouch.ux.ui

import com.sensitouch.ux.data.ResumenDia
import com.sensitouch.ux.data.Voto
import com.sensitouch.ux.data.VotosHora
import com.sensitouch.ux.ui.componentes.BarraApilada
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

val LOCAL_PE: Locale = Locale.forLanguageTag("es-PE")

fun fechaLarga(fecha: LocalDate = LocalDate.now()): String =
    fecha.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", LOCAL_PE))
        .replaceFirstChar { it.titlecase(LOCAL_PE) }

fun horaDe(milis: Long): String =
    Instant.ofEpochMilli(milis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm:ss"))

fun fechaHoraCorta(milis: Long): String {
    val momento = Instant.ofEpochMilli(milis).atZone(ZoneId.systemDefault()).toLocalDateTime()
    val hoy = LocalDate.now()
    val dia = when (momento.toLocalDate()) {
        hoy -> "Hoy"
        hoy.minusDays(1) -> "Ayer"
        else -> momento.format(DateTimeFormatter.ofPattern("d MMM", LOCAL_PE))
    }
    return "$dia, ${momento.format(DateTimeFormatter.ofPattern("HH:mm"))}"
}

fun esDeHoy(milis: Long): Boolean =
    Instant.ofEpochMilli(milis).atZone(ZoneId.systemDefault()).toLocalDate() == LocalDate.now()

/** "hace 12 s", "hace 5 min" o la hora del voto si el ESP32 no tenia hora de internet. */
fun haceCuanto(voto: Voto, ahoraSeg: Long = System.currentTimeMillis() / 1000): String {
    if (voto.epoch <= 0) return voto.hora
    val dif = ahoraSeg - voto.epoch
    return when {
        dif < 5 -> "ahora"
        dif < 60 -> "hace $dif s"
        dif < 3600 -> "hace ${dif / 60} min"
        else -> voto.hora
    }
}

fun calidadSenal(rssi: Int): String = when {
    rssi == 0 -> "Sin dato"
    rssi >= -60 -> "Excelente ($rssi dBm)"
    rssi >= -70 -> "Buena ($rssi dBm)"
    rssi >= -80 -> "Regular ($rssi dBm)"
    else -> "Débil ($rssi dBm)"
}

/** Barras de hoy desde la primera hora con votos hasta la hora actual. */
fun barrasDeHoy(porHora: List<VotosHora>): List<BarraApilada> {
    if (porHora.isEmpty()) return emptyList()
    val conVotos = porHora.filter { it.total > 0 }
    if (conVotos.isEmpty()) return emptyList()
    val desde = minOf(conVotos.minOf { it.hora }, 8)
    val hasta = maxOf(conVotos.maxOf { it.hora }, minOf(LocalDateTime.now().hour, 23))
    return (desde..hasta).map { h ->
        val v = porHora.firstOrNull { it.hora == h } ?: VotosHora(h, 0, 0, 0)
        BarraApilada("${h}h", v.malo, v.regular, v.bueno)
    }
}

/** Los ultimos [dias] dias (incluido hoy) aunque algun dia no tenga datos. */
fun ultimosDias(historial: List<ResumenDia>, dias: Int, hasta: LocalDate = LocalDate.now()): List<ResumenDia> =
    (dias - 1 downTo 0).map { atras ->
        val fecha = hasta.minusDays(atras.toLong()).toString()
        historial.firstOrNull { it.fecha == fecha } ?: ResumenDia(fecha, 0, 0, 0)
    }

fun etiquetaDia(fecha: String, corta: Boolean): String {
    val dia = runCatching { LocalDate.parse(fecha) }.getOrNull() ?: return fecha
    return if (corta) dia.dayOfWeek.getDisplayName(TextStyle.SHORT, LOCAL_PE).take(3)
    else dia.dayOfMonth.toString()
}

fun nombreDia(fecha: String): String {
    val dia = runCatching { LocalDate.parse(fecha) }.getOrNull() ?: return fecha
    return dia.format(DateTimeFormatter.ofPattern("EEEE d", LOCAL_PE))
}
