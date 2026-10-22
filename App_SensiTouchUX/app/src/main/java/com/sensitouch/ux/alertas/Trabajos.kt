package com.sensitouch.ux.alertas

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sensitouch.ux.SensiTouchApp
import com.sensitouch.ux.data.Opinion
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Revisa el ESP32 aunque la app esté cerrada (cada 15 min, el minimo que permite Android).
 * Si encuentra una alerta nueva, manda la notificacion al gerente.
 */
class MonitorWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as SensiTouchApp
        val preferencias = app.preferencias
        if (!preferencias.sesionActiva.first()) return Result.success()
        val dispositivo = preferencias.dispositivo.first()
        if (!dispositivo.vinculado) return Result.success()
        val config = preferencias.configAlertas.first()

        val nuevas = try {
            val estado = app.repositorio.leerEstado(dispositivo)
            app.repositorio.procesar(estado)
        } catch (e: Exception) {
            app.repositorio.revisarDesconexion()
        }
        if (config.notificaciones) {
            nuevas.forEach { Notificaciones.mostrarAlerta(applicationContext, it) }
        }
        return Result.success()
    }
}

/** Manda el resumen del dia a la hora que eligio el gerente (por defecto 9 p. m.). */
class ResumenWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as SensiTouchApp
        val preferencias = app.preferencias
        val config = preferencias.configAlertas.first()
        if (!preferencias.sesionActiva.first() || !config.resumenDiario) return Result.success()

        val dispositivo = preferencias.dispositivo.first()
        val estado = runCatching { app.repositorio.leerEstado(dispositivo) }.getOrNull()
        if (estado != null) app.repositorio.procesar(estado)

        val hoy = LocalDate.now().toString()
        val dia = preferencias.historial.first().firstOrNull { it.fecha == hoy }
        val alertasHoy = preferencias.alertas.first().count {
            Instant.ofEpochMilli(it.fechaHora).atZone(ZoneId.systemDefault()).toLocalDate().toString() == hoy
        }
        if (dia == null || dia.total == 0) {
            Notificaciones.mostrarResumen(applicationContext, "Resumen de hoy", "Hoy no se registraron opiniones.")
            return Result.success()
        }
        val pctMalo = dia.malo * 100 / dia.total
        val pctBueno = dia.bueno * 100 / dia.total
        val texto = buildString {
            append("${dia.total} opiniones: ${dia.bueno} buenas ($pctBueno %), ${dia.regular} regulares y ${dia.malo} malas ($pctMalo %).")
            if (estado != null && estado.horaConMasQuejas != null) {
                append(" Más quejas entre las ${EvaluadorAlertas.rango(estado.horaConMasQuejas!!)}.")
            }
            if (alertasHoy > 0) append(" Hubo $alertasHoy alerta(s) hoy.")
        }
        val pctActual = estado?.porcentaje(Opinion.MALO) ?: pctMalo
        val emoji = if (pctActual >= config.umbralPorcentaje) "⚠️ " else ""
        Notificaciones.mostrarResumen(applicationContext, "${emoji}Resumen de hoy en tu local", texto)
        return Result.success()
    }
}

object Programador {

    private const val MONITOR = "sensitouch-monitor"
    private const val RESUMEN = "sensitouch-resumen"

    fun programarMonitor(context: Context) {
        val pedido = PeriodicWorkRequestBuilder<MonitorWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(MONITOR, ExistingPeriodicWorkPolicy.KEEP, pedido)
    }

    fun programarResumen(context: Context, activo: Boolean, hora: Int) {
        val gestor = WorkManager.getInstance(context)
        if (!activo) {
            gestor.cancelUniqueWork(RESUMEN)
            return
        }
        val ahora = LocalDateTime.now()
        var siguiente = ahora.withHour(hora).withMinute(0).withSecond(0).withNano(0)
        if (!siguiente.isAfter(ahora)) siguiente = siguiente.plusDays(1)
        val espera = Duration.between(ahora, siguiente).toMinutes()

        val pedido = PeriodicWorkRequestBuilder<ResumenWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(espera, TimeUnit.MINUTES)
            .build()
        // se vuelve a programar para que respete la hora elegida
        gestor.enqueueUniquePeriodicWork(RESUMEN, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, pedido)
    }

    fun cancelarTodo(context: Context) {
        WorkManager.getInstance(context).apply {
            cancelUniqueWork(MONITOR)
            cancelUniqueWork(RESUMEN)
        }
    }
}
