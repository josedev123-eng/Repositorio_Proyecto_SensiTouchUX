package com.sensitouch.ux

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.sensitouch.ux.alertas.Notificaciones
import com.sensitouch.ux.ui.AppViewModel
import com.sensitouch.ux.ui.navegacion.AppNavegacion
import com.sensitouch.ux.ui.theme.SensiTouchTheme

class MainActivity : ComponentActivity() {

    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // la pantalla de inicio se queda hasta saber si hay sesion
        installSplashScreen().setKeepOnScreenCondition { vm.ui.value.inicio == null }
        super.onCreate(savedInstanceState)
        // encabezados azules: iconos de la barra de estado en blanco
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))

        if (savedInstanceState == null) vm.abrirDesdeNotificacion(intent.getStringExtra(Notificaciones.EXTRA_ABRIR))

        setContent {
            SensiTouchTheme {
                AppNavegacion(vm)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        vm.abrirDesdeNotificacion(intent.getStringExtra(Notificaciones.EXTRA_ABRIR))
    }

    override fun onStart() {
        super.onStart()
        vm.alEntrar()
    }

    override fun onStop() {
        super.onStop()
        vm.alSalir()
    }
}
