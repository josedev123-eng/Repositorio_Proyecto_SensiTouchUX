package com.sensitouch.ux.ui.pantallas

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Accessible
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Store
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.sensitouch.ux.alertas.Notificaciones
import com.sensitouch.ux.data.ConfigAlertas
import com.sensitouch.ux.ui.AppViewModel
import com.sensitouch.ux.ui.componentes.EncabezadoGradiente
import com.sensitouch.ux.ui.componentes.TarjetaSeccion
import com.sensitouch.ux.ui.theme.ColorMalo
import kotlin.math.roundToInt

@Composable
fun PantallaAjustes(vm: AppViewModel, alCambiarDispositivo: () -> Unit, alCerrarSesion: () -> Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val e = ui.estado
    val c = ui.config
    val context = LocalContext.current
    val foco = LocalFocusManager.current

    var confirmarReinicio by remember { mutableStateOf(false) }
    var confirmarSalida by remember { mutableStateOf(false) }
    var mensaje by rememberSaveable { mutableStateOf("") }

    // Se vuelve a revisar el permiso cada vez que el gerente regresa de los ajustes del celular
    var notificacionesSistema by remember { mutableStateOf(Notificaciones.activadasEnSistema(context)) }
    val ciclo = LocalLifecycleOwner.current
    LaunchedEffect(ciclo) {
        ciclo.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            notificacionesSistema = Notificaciones.activadasEnSistema(context)
        }
    }

    fun guardar(nueva: ConfigAlertas) = vm.guardarConfig(nueva)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        EncabezadoGradiente(titulo = "Ajustes", subtitulo = "Dispositivo, alertas y cuenta")

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

            // ---------------- Alertas ----------------
            TarjetaSeccion("Alertas para el gerente", icono = Icons.Rounded.NotificationsActive) {
                if (!notificacionesSistema) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                        modifier = Modifier.padding(bottom = 8.dp),
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.NotificationsOff, contentDescription = null)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Las notificaciones están bloqueadas en tu celular. Actívalas para enterarte al momento.",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                )
                            }) { Text("Activar") }
                        }
                    }
                }

                FilaInterruptor(
                    Icons.Rounded.NotificationsActive,
                    "Notificaciones",
                    "Te avisa aunque la app esté cerrada (se revisa cada 15 min).",
                    c.notificaciones,
                ) { guardar(c.copy(notificaciones = it)) }

                Separador()

                // Limite de opiniones "Malo"
                var umbral by remember(c.umbralPorcentaje) { mutableFloatStateOf(c.umbralPorcentaje.toFloat()) }
                Text("Límite de opiniones \"Malo\"", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Avisar cuando las quejas del día lleguen al ${umbral.roundToInt()} % de los votos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Slider(
                        value = umbral,
                        onValueChange = { umbral = it },
                        onValueChangeFinished = { guardar(c.copy(umbralPorcentaje = umbral.roundToInt())) },
                        valueRange = 10f..80f,
                        steps = 13,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("${umbral.roundToInt()} %", style = MaterialTheme.typography.titleMedium, color = ColorMalo)
                }

                Contador(
                    "Mínimo de votos para avisar",
                    "Evita alertas falsas con muy pocos votos.",
                    c.minimoVotos,
                    1..50,
                ) { guardar(c.copy(minimoVotos = it)) }

                Separador()

                FilaInterruptor(
                    Icons.Rounded.Bolt,
                    "Quejas seguidas",
                    "Avisa si llegan varios \"Malo\" en pocos minutos, aunque el día vaya bien.",
                    c.rachaActiva,
                ) { guardar(c.copy(rachaActiva = it)) }
                AnimatedVisibility(c.rachaActiva) {
                    Column {
                        Contador("Cantidad de quejas", null, c.rachaCantidad, 2..6) { guardar(c.copy(rachaCantidad = it)) }
                        Contador("Dentro de (minutos)", null, c.rachaMinutos, 5..30, paso = 5) { guardar(c.copy(rachaMinutos = it)) }
                    }
                }

                Separador()

                FilaInterruptor(
                    Icons.Rounded.LinkOff,
                    "Si el SensiTouch se desconecta",
                    "Avisa si pasa más de 30 min sin responder.",
                    c.avisoDesconexion,
                ) { guardar(c.copy(avisoDesconexion = it)) }

                FilaInterruptor(
                    Icons.Rounded.Bedtime,
                    "Resumen del día",
                    "Un resumen con los votos y las alertas a la hora que elijas.",
                    c.resumenDiario,
                ) { guardar(c.copy(resumenDiario = it)) }
                AnimatedVisibility(c.resumenDiario) {
                    Row(
                        Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(start = 40.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        (18..23).forEach { h ->
                            FilterChip(
                                selected = c.horaResumen == h,
                                onClick = { guardar(c.copy(horaResumen = h)) },
                                label = { Text(if (h > 12) "${h - 12} p. m." else "$h") },
                            )
                        }
                    }
                }

                Spacer(Modifier.size(8.dp))
                OutlinedButton(onClick = vm::probarNotificacion, modifier = Modifier.fillMaxWidth()) {
                    Text("Probar notificación")
                }
            }

            // ---------------- Dispositivo ----------------
            TarjetaSeccion("Dispositivo SensiTouch", icono = Icons.Rounded.Router) {
                FilaInterruptor(
                    Icons.AutoMirrored.Rounded.VolumeUp,
                    "Voz",
                    "Da la bienvenida y dice \"Gracias por tu opinión\".",
                    e.voz,
                ) { vm.configurar(voz = it) }
                FilaInterruptor(
                    Icons.Rounded.Accessible,
                    "Botón accesible",
                    "Menú por voz para personas con discapacidad visual.",
                    e.modoAccesible,
                ) { vm.configurar(accesible = it) }
                FilaInterruptor(
                    Icons.Rounded.Lightbulb,
                    "LED de confirmación",
                    "Prende el color del voto por un momento.",
                    e.led,
                ) { vm.configurar(led = it) }

                Separador()

                OutlinedTextField(
                    value = mensaje,
                    onValueChange = { if (it.length <= 20) mensaje = it },
                    label = { Text("Mensaje en la pantalla") },
                    placeholder = { Text(e.mensaje) },
                    supportingText = { Text("${mensaje.length}/20 · se muestra en la OLED del dispositivo") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (mensaje.isNotBlank()) { vm.configurar(mensaje = mensaje.trim()); mensaje = ""; foco.clearFocus() }
                    }),
                    trailingIcon = {
                        IconButton(
                            onClick = { vm.configurar(mensaje = mensaje.trim()); mensaje = ""; foco.clearFocus() },
                            enabled = mensaje.isNotBlank(),
                        ) { Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Enviar mensaje") }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                FilledTonalButton(onClick = { confirmarReinicio = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Reiniciar votos de hoy")
                }
            }

            // ---------------- Demo ----------------
            if (ui.dispositivo.demo) {
                TarjetaSeccion("Modo demostración", icono = Icons.Rounded.Science) {
                    Text(
                        "Para la exposición: agrega varias opiniones \"Malo\" seguidas y mira cómo salta la alerta.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.size(8.dp))
                    FilledTonalButton(onClick = vm::simularRachaMala, modifier = Modifier.fillMaxWidth()) {
                        Text("Simular quejas seguidas")
                    }
                }
            }

            // ---------------- Cuenta ----------------
            TarjetaSeccion("Cuenta", icono = Icons.Rounded.Store) {
                ListItem(
                    headlineContent = { Text(ui.negocio?.nombre ?: "Mi negocio") },
                    supportingContent = { Text(ui.negocio?.correo ?: "") },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
                OpcionCuenta(Icons.Rounded.SwapHoriz, "Cambiar dispositivo") { vm.cambiarDispositivo(alCambiarDispositivo) }
                OpcionCuenta(Icons.Rounded.DeleteSweep, "Borrar historial de alertas") { vm.borrarAlertas() }
                OpcionCuenta(Icons.AutoMirrored.Rounded.Logout, "Cerrar sesión", color = ColorMalo) { confirmarSalida = true }
            }

            Text(
                "SensiTouch UX 1.0 · Grupo 8 · TECSUP",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }

    if (confirmarReinicio) {
        AlertDialog(
            onDismissRequest = { confirmarReinicio = false },
            icon = { Icon(Icons.Rounded.RestartAlt, contentDescription = null) },
            title = { Text("¿Reiniciar los votos?") },
            text = { Text("Los contadores del SensiTouch volverán a cero. El resumen de hoy que ya está en el celular se mantiene.") },
            confirmButton = {
                TextButton(onClick = { confirmarReinicio = false; vm.reiniciarVotos() }) { Text("Reiniciar") }
            },
            dismissButton = { TextButton(onClick = { confirmarReinicio = false }) { Text("Cancelar") } },
        )
    }

    if (confirmarSalida) {
        AlertDialog(
            onDismissRequest = { confirmarSalida = false },
            title = { Text("¿Cerrar sesión?") },
            text = { Text("Dejarás de recibir alertas en este celular hasta que vuelvas a ingresar.") },
            confirmButton = {
                TextButton(onClick = { confirmarSalida = false; vm.cerrarSesion(alCerrarSesion) }) { Text("Cerrar sesión") }
            },
            dismissButton = { TextButton(onClick = { confirmarSalida = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun FilaInterruptor(
    icono: ImageVector,
    titulo: String,
    descripcion: String,
    activo: Boolean,
    alCambiar: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(titulo) },
        supportingContent = { Text(descripcion) },
        leadingContent = { Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
        trailingContent = { Switch(checked = activo, onCheckedChange = alCambiar) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.padding(horizontal = 0.dp),
    )
}

@Composable
private fun Contador(
    titulo: String,
    descripcion: String?,
    valor: Int,
    rango: IntRange,
    paso: Int = 1,
    alCambiar: (Int) -> Unit,
) {
    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.titleSmall)
            if (descripcion != null) {
                Text(descripcion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        OutlinedIconButton(
            onClick = { alCambiar((valor - paso).coerceIn(rango)) },
            enabled = valor > rango.first,
            modifier = Modifier.size(36.dp),
        ) { Icon(Icons.Rounded.Remove, contentDescription = "Menos") }
        Text(
            "$valor",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.width(44.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        OutlinedIconButton(
            onClick = { alCambiar((valor + paso).coerceIn(rango)) },
            enabled = valor < rango.last,
            modifier = Modifier.size(36.dp),
        ) { Icon(Icons.Rounded.Add, contentDescription = "Más") }
    }
}

@Composable
private fun OpcionCuenta(icono: ImageVector, texto: String, color: Color = Color.Unspecified, alPresionar: () -> Unit) {
    TextButton(onClick = alPresionar, modifier = Modifier.fillMaxWidth()) {
        Icon(icono, contentDescription = null, tint = if (color == Color.Unspecified) MaterialTheme.colorScheme.primary else color)
        Spacer(Modifier.width(12.dp))
        Text(
            texto,
            color = if (color == Color.Unspecified) MaterialTheme.colorScheme.primary else color,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun Separador() {
    HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
}
