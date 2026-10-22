package com.sensitouch.ux

import android.app.Application
import com.sensitouch.ux.alertas.Notificaciones
import com.sensitouch.ux.alertas.Programador
import com.sensitouch.ux.data.Preferencias
import com.sensitouch.ux.data.Repositorio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SensiTouchApp : Application() {

    lateinit var preferencias: Preferencias
        private set
    lateinit var repositorio: Repositorio
        private set

    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        preferencias = Preferencias(this)
        repositorio = Repositorio(preferencias)
        Notificaciones.crearCanales(this)

        // Revision en segundo plano para avisar aunque la app este cerrada
        Programador.programarMonitor(this)
        alcance.launch {
            val config = preferencias.configAlertas.first()
            Programador.programarResumen(this@SensiTouchApp, config.resumenDiario, config.horaResumen)
        }
    }
}
