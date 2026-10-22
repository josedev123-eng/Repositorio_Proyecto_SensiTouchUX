package com.sensitouch.ux.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.random.Random

/**
 * SensiTouch simulado. Sirve para probar y presentar la app sin el ESP32:
 * genera votos solos cada pocos segundos y responde igual que el /estado real.
 */
class DemoDispositivo {

    private val votos = mutableListOf<Voto>()
    private var siguienteId = 1
    private var proximoVoto = 0L
    private var dia = ""
    private val azar = Random(System.currentTimeMillis())

    private var voz = true
    private var led = true
    private var modoAccesible = true
    private var mensaje = "Selecciona tu opinion"

    @Synchronized
    fun estado(): EstadoDispositivo {
        val hoy = LocalDate.now().toString()
        if (dia != hoy) sembrar(hoy)
        generarNuevos()
        return construir()
    }

    @Synchronized
    fun configurar(voz: Boolean?, led: Boolean?, accesible: Boolean?, mensaje: String?) {
        voz?.let { this.voz = it }
        led?.let { this.led = it }
        accesible?.let { this.modoAccesible = it }
        mensaje?.let { this.mensaje = it }
    }

    @Synchronized
    fun reiniciar() {
        votos.clear()
        siguienteId = 1
    }

    /** Agrega varias opiniones "Malo" seguidas para mostrar como salta la alerta. */
    @Synchronized
    fun simularRachaMala(cantidad: Int = 4) {
        val ahora = LocalDateTime.now()
        repeat(cantidad) { i ->
            agregar(Opinion.MALO, ahora.minusSeconds(((cantidad - i) * 40).toLong()), porBoton = i == 1)
        }
    }

    // Votos de la manana hasta ahora, con mas "Bueno" que "Malo" como un local normal
    private fun sembrar(hoy: String) {
        dia = hoy
        votos.clear()
        siguienteId = 1
        val ahora = LocalDateTime.now()
        val desde = if (ahora.hour >= 9) 8 else 0
        for (h in desde..ahora.hour) {
            val cantidad = if (h in 12..14 || h in 18..20) azar.nextInt(5, 10) else azar.nextInt(1, 6)
            repeat(cantidad) {
                val momento = ahora.withHour(h).withMinute(azar.nextInt(0, 60)).withSecond(azar.nextInt(0, 60))
                if (momento.isBefore(ahora)) agregar(opinionAlAzar(h), momento, azar.nextInt(10) == 0)
            }
        }
        // ordena por hora y renumera para que los id sigan el orden en que llegaron
        val ordenados = votos.sortedBy { it.epoch }.mapIndexed { i, v -> v.copy(id = i + 1) }
        votos.clear()
        votos.addAll(ordenados)
        siguienteId = votos.size + 1
        proximoVoto = System.currentTimeMillis() + 6000
    }

    private fun generarNuevos() {
        val ahora = System.currentTimeMillis()
        if (ahora < proximoVoto) return
        agregar(opinionAlAzar(LocalDateTime.now().hour), LocalDateTime.now(), azar.nextInt(8) == 0)
        proximoVoto = ahora + azar.nextLong(7000, 16000)
    }

    // A la hora del almuerzo hay mas quejas (para que el reporte muestre algo interesante)
    private fun opinionAlAzar(hora: Int): Opinion {
        val pMalo = if (hora in 12..13) 30 else 14
        val n = azar.nextInt(100)
        return when {
            n < pMalo -> Opinion.MALO
            n < pMalo + 26 -> Opinion.REGULAR
            else -> Opinion.BUENO
        }
    }

    private fun agregar(opinion: Opinion, momento: LocalDateTime, porBoton: Boolean) {
        votos += Voto(
            id = siguienteId++,
            opinion = opinion,
            hora = momento.format(FORMATO_HORA),
            porBoton = porBoton,
            epoch = momento.atZone(ZoneId.systemDefault()).toEpochSecond(),
        )
    }

    private fun construir(): EstadoDispositivo {
        val ahora = LocalDateTime.now()
        val porHora = (0..23).map { h ->
            val deEsaHora = votos.filter { it.hora.take(2).toIntOrNull() == h }
            VotosHora(
                hora = h,
                malo = deEsaHora.count { it.opinion == Opinion.MALO },
                regular = deEsaHora.count { it.opinion == Opinion.REGULAR },
                bueno = deEsaHora.count { it.opinion == Opinion.BUENO },
            )
        }
        return EstadoDispositivo(
            malo = votos.count { it.opinion == Opinion.MALO },
            regular = votos.count { it.opinion == Opinion.REGULAR },
            bueno = votos.count { it.opinion == Opinion.BUENO },
            accesible = votos.count { it.porBoton },
            modoAccesible = modoAccesible,
            voz = voz,
            led = led,
            mensaje = mensaje,
            hora = ahora.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
            fecha = ahora.toLocalDate().toString(),
            rssi = -55 - azar.nextInt(10),
            ip = "demo",
            dfplayer = true,
            oled = true,
            porHora = porHora,
            ultimos = votos.sortedByDescending { it.epoch }.take(20),
        )
    }

    companion object {
        private val FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm")

        /** Dias anteriores inventados para que los reportes de semana y mes tengan datos en la demo. */
        fun historialSimulado(dias: Int): List<ResumenDia> {
            val azar = Random(42)
            val hoy = LocalDate.now()
            return (1..dias).map { atras ->
                val fecha = hoy.minusDays(atras.toLong())
                val finDeSemana = fecha.dayOfWeek.value >= 6
                val total = if (finDeSemana) azar.nextInt(70, 110) else azar.nextInt(35, 75)
                val malo = (total * azar.nextInt(6, 28) / 100f).toInt()
                val regular = (total * azar.nextInt(15, 30) / 100f).toInt()
                ResumenDia(fecha.toString(), malo, regular, total - malo - regular)
            }
        }
    }
}
