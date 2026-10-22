package com.sensitouch.ux.ui.navegacion

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.HowToVote
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.HowToVote
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sensitouch.ux.alertas.Notificaciones
import com.sensitouch.ux.data.TipoAlerta
import com.sensitouch.ux.ui.AppViewModel
import com.sensitouch.ux.ui.pantallas.PantallaAjustes
import com.sensitouch.ux.ui.pantallas.PantallaAlertas
import com.sensitouch.ux.ui.pantallas.PantallaLogin
import com.sensitouch.ux.ui.pantallas.PantallaPanel
import com.sensitouch.ux.ui.pantallas.PantallaRegistro
import com.sensitouch.ux.ui.pantallas.PantallaReportes
import com.sensitouch.ux.ui.pantallas.PantallaVincular
import com.sensitouch.ux.ui.pantallas.PantallaVotos

object Rutas {
    const val LOGIN = "login"
    const val REGISTRO = "registro"
    const val VINCULAR = "vincular"
    const val PANEL = "panel"
    const val VOTOS = "votos"
    const val REPORTES = "reportes"
    const val AJUSTES = "ajustes"
    const val ALERTAS = "alertas"
}

private data class Pestana(val ruta: String, val etiqueta: String, val icono: ImageVector, val iconoActivo: ImageVector)

private val PESTANAS = listOf(
    Pestana(Rutas.PANEL, "Panel", Icons.Outlined.Dashboard, Icons.Rounded.Dashboard),
    Pestana(Rutas.VOTOS, "Votos", Icons.Outlined.HowToVote, Icons.Rounded.HowToVote),
    Pestana(Rutas.REPORTES, "Reportes", Icons.Outlined.BarChart, Icons.Rounded.BarChart),
    Pestana(Rutas.AJUSTES, "Ajustes", Icons.Outlined.Settings, Icons.Rounded.Settings),
)

private val RUTAS_CON_SESION = PESTANAS.map { it.ruta } + Rutas.ALERTAS

/** Cambia de pestaña sin apilar pantallas (como en cualquier app con barra inferior). */
private fun NavHostController.irAPestana(ruta: String) {
    navigate(ruta) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Limpia todo el historial y abre [ruta] (al iniciar o cerrar sesion). */
private fun NavHostController.empezarEn(ruta: String) {
    navigate(ruta) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}

@Composable
fun AppNavegacion(vm: AppViewModel) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val inicio = ui.inicio ?: return

    val nav = rememberNavController()
    val entrada by nav.currentBackStackEntryAsState()
    val rutaActual = entrada?.destination?.route
    val avisos = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        vm.mensajes.collect { avisos.showSnackbar(it) }
    }

    // Si se toco una notificacion, abre la pantalla que corresponde
    val destino by vm.destino.collectAsStateWithLifecycle()
    LaunchedEffect(destino, rutaActual) {
        val d = destino ?: return@LaunchedEffect
        if (rutaActual !in RUTAS_CON_SESION) return@LaunchedEffect
        if (d == Rutas.ALERTAS) nav.navigate(Rutas.ALERTAS) { launchSingleTop = true } else nav.irAPestana(d)
        vm.destinoAbierto()
    }

    // Android 13+: pide permiso para notificaciones la primera vez que se entra al panel
    var permisoPedido by rememberSaveable { mutableStateOf(false) }
    val lanzadorPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(rutaActual) {
        if (rutaActual == Rutas.PANEL && !permisoPedido &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !Notificaciones.permisoConcedido(context)
        ) {
            permisoPedido = true
            lanzadorPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(avisos) },
        bottomBar = {
            if (rutaActual in PESTANAS.map { it.ruta }) {
                NavigationBar {
                    PESTANAS.forEach { p ->
                        val activa = rutaActual == p.ruta
                        NavigationBarItem(
                            selected = activa,
                            onClick = { nav.irAPestana(p.ruta) },
                            icon = { Icon(if (activa) p.iconoActivo else p.icono, contentDescription = null) },
                            label = { Text(p.etiqueta) },
                        )
                    }
                }
            }
        },
    ) { relleno ->
        NavHost(
            navController = nav,
            startDestination = inicio,
            modifier = Modifier.padding(relleno),
            enterTransition = { fadeIn(tween(220)) },
            exitTransition = { fadeOut(tween(180)) },
        ) {
            composable(Rutas.LOGIN) {
                PantallaLogin(
                    vm = vm,
                    alEntrar = { ruta -> nav.empezarEn(ruta) },
                    alCrearCuenta = { nav.navigate(Rutas.REGISTRO) },
                )
            }
            composable(Rutas.REGISTRO) {
                PantallaRegistro(
                    vm = vm,
                    alVolver = { nav.popBackStack() },
                    alTerminar = { nav.empezarEn(Rutas.VINCULAR) },
                )
            }
            composable(Rutas.VINCULAR) {
                PantallaVincular(vm = vm, alTerminar = { nav.empezarEn(Rutas.PANEL) })
            }
            composable(Rutas.PANEL) {
                PantallaPanel(
                    vm = vm,
                    alVerAlertas = { nav.navigate(Rutas.ALERTAS) },
                    alVerVotos = { nav.irAPestana(Rutas.VOTOS) },
                    alVerReportes = { nav.irAPestana(Rutas.REPORTES) },
                )
            }
            composable(Rutas.VOTOS) { PantallaVotos(vm = vm) }
            composable(Rutas.REPORTES) { PantallaReportes(vm = vm) }
            composable(Rutas.AJUSTES) {
                PantallaAjustes(
                    vm = vm,
                    alCambiarDispositivo = { nav.empezarEn(Rutas.VINCULAR) },
                    alCerrarSesion = { nav.empezarEn(Rutas.LOGIN) },
                )
            }
            composable(Rutas.ALERTAS) {
                PantallaAlertas(vm = vm, alVolver = { nav.popBackStack() })
            }
        }
    }

    // Aviso grande al entrar o cuando llega una alerta con la app abierta
    val dialogo = ui.dialogo
    if (dialogo != null && rutaActual in RUTAS_CON_SESION) {
        AlertDialog(
            onDismissRequest = vm::cerrarDialogo,
            icon = {
                Icon(
                    when (dialogo.tipo) {
                        TipoAlerta.RACHA -> Icons.Rounded.Bolt
                        TipoAlerta.DESCONECTADO -> Icons.Rounded.LinkOff
                        else -> Icons.Rounded.WarningAmber
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            title = { Text(dialogo.titulo) },
            text = { Text(dialogo.mensaje) },
            confirmButton = {
                TextButton(onClick = {
                    vm.cerrarDialogo()
                    if (rutaActual != Rutas.ALERTAS) nav.navigate(Rutas.ALERTAS)
                }) { Text("Ver qué pasó") }
            },
            dismissButton = {
                TextButton(onClick = vm::cerrarDialogo) { Text("Entendido") }
            },
        )
    }
}
