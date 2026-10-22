package com.sensitouch.ux.alertas

import com.sensitouch.ux.data.Alerta
import com.sensitouch.ux.data.ConfigAlertas
import com.sensitouch.ux.data.EstadoDispositivo
import com.sensitouch.ux.data.Opinion
import com.sensitouch.ux.data.TipoAlerta
import com.sensitouch.ux.data.Voto
import java.time.LocalDate

/**
 * Decide si hay que avisar al gerente. Hay dos reglas:
 *
 *  1) Porcentaje: los votos "Malo" del dia superan el limite (ej. 30 %) y ya hay
 *     suficientes votos para que el porcentaje signifique algo (ej. 5 votos).
 *  2) Racha: llegan varios "Malo" seguidos en pocos minutos (ej. 3 en 10 min).
 *     Esto detecta un problema puntual de la manana aunque el dia vaya bien.
 *
 * Cada alerta tiene un id fijo para no repetir el aviso.
 */
object EvaluadorAlertas {

    fun evaluar(estado: EstadoDispositivo, config: ConfigAlertas, ahora: Long = System.currentTimeMillis()): List<Alerta> {
        val fecha = estado.fecha.ifBlank { LocalDate.now().toString() }
        return listOfNotNull(
            alertaPorcentaje(estado, config, fecha, ahora),
            if (config.rachaActiva) alertaRacha(estado, config, fecha, ahora) else null,
        )
    }

    fun superaLimite(estado: EstadoDispositivo, config: ConfigAlertas): Boolean =
        estado.total >= config.minimoVotos && estado.porcentaje(Opinion.MALO) >= config.umbralPorcentaje

    private fun alertaPorcentaje(estado: EstadoDispositivo, config: ConfigAlertas, fecha: String, ahora: Long): Alerta? {
        if (!superaLimite(estado, config)) return null
        val pct = estado.porcentaje(Opinion.MALO)
        val horaPico = estado.horaConMasQuejas
        val detalle = buildString {
            append("Hoy van ${estado.malo} opiniones \"Malo\" de ${estado.total} (${pct} %). ")
            append("Tu límite es ${config.umbralPorcentaje} %. ")
            if (horaPico != null) {
                append("La mayoría de quejas llegó entre las ${rango(horaPico)}. ")
            }
            append("Revisa qué pasó en ese horario: tiempo de espera, atención o limpieza.")
        }
        return Alerta(
            // sube un escalon cada 10 %: 30 %, 40 %, 50 %... para avisar si empeora
            id = "pct-$fecha-${pct / 10}",
            tipo = TipoAlerta.PORCENTAJE,
            titulo = "Opiniones negativas altas: $pct %",
            detalle = detalle,
            fechaHora = ahora,
        )
    }

    private fun alertaRacha(estado: EstadoDispositivo, config: ConfigAlertas, fecha: String, ahora: Long): Alerta? {
        val malos = estado.ultimos
            .filter { it.opinion == Opinion.MALO }
            .sortedByDescending { segundos(it) }
            .take(config.rachaCantidad)
        if (malos.size < config.rachaCantidad || config.rachaCantidad < 2) return null

        val masNuevo = malos.first()
        val masViejo = malos.last()
        val diferencia = segundos(masNuevo) - segundos(masViejo)
        if (diferencia > config.rachaMinutos * 60L) return null

        val minutos = maxOf(1L, (diferencia + 59) / 60)
        return Alerta(
            id = "racha-$fecha-${masViejo.id}",
            tipo = TipoAlerta.RACHA,
            titulo = "${malos.size} opiniones \"Malo\" seguidas",
            detalle = "Entre las ${masViejo.hora} y las ${masNuevo.hora} llegaron ${malos.size} votos negativos " +
                "en $minutos min. Puede ser un problema puntual que conviene revisar ahora.",
            fechaHora = ahora,
        )
    }

    /** Segundos del voto: usa la hora de internet del ESP32 o, si no tiene, la hora HH:mm. */
    private fun segundos(voto: Voto): Long {
        if (voto.epoch > 0) return voto.epoch
        val partes = voto.hora.split(":")
        val h = partes.getOrNull(0)?.toLongOrNull() ?: 0
        val m = partes.getOrNull(1)?.toLongOrNull() ?: 0
        val s = partes.getOrNull(2)?.toLongOrNull() ?: 0
        return h * 3600 + m * 60 + s
    }

    fun rango(hora: Int): String = "%02d:00 y %02d:00".format(hora, (hora + 1) % 24)
}
