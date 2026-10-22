package com.sensitouch.ux.ui.pantallas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Accessible
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.HowToVote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SentimentSatisfied
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sensitouch.ux.data.EstadoConexion
import com.sensitouch.ux.data.Opinion
import com.sensitouch.ux.data.Voto
import com.sensitouch.ux.ui.AppViewModel
import com.sensitouch.ux.ui.barrasDeHoy
import com.sensitouch.ux.ui.calidadSenal
import com.sensitouch.ux.ui.componentes.BannerAlerta
import com.sensitouch.ux.ui.componentes.ChipEstadoConexion
import com.sensitouch.ux.ui.componentes.EncabezadoGradiente
import com.sensitouch.ux.ui.componentes.FilaOpinion
import com.sensitouch.ux.ui.componentes.GraficoBarrasApiladas
import com.sensitouch.ux.ui.componentes.GraficoDona
import com.sensitouch.ux.ui.componentes.TarjetaMetrica
import com.sensitouch.ux.ui.componentes.TarjetaSeccion
import com.sensitouch.ux.ui.componentes.colorOpinion
import com.sensitouch.ux.ui.componentes.iconoOpinion
import com.sensitouch.ux.ui.componentes.iconoOrigen
import com.sensitouch.ux.ui.esDeHoy
import com.sensitouch.ux.ui.fechaLarga
import com.sensitouch.ux.ui.haceCuanto
import com.sensitouch.ux.ui.horaDe
import com.sensitouch.ux.ui.theme.AzulSensi
import com.sensitouch.ux.ui.theme.ColorAccesible
import com.sensitouch.ux.ui.theme.ColorBueno
import com.sensitouch.ux.ui.theme.ColorMalo
import com.sensitouch.ux.ui.theme.ColorRegular

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPanel(
    vm: AppViewModel,
    alVerAlertas: () -> Unit,
    alVerVotos: () -> Unit,
    alVerReportes: () -> Unit,
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val e = ui.estado
    val alertasHoy = ui.alertas.count { esDeHoy(it.fechaHora) }

    PullToRefreshBox(
        isRefreshing = ui.actualizando,
        onRefresh = vm::refrescar,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                EncabezadoGradiente(
                    titulo = "Hola, ${ui.negocio?.nombre ?: "gerente"}",
                    subtitulo = fechaLarga(),
                    acciones = {
                        IconButton(onClick = alVerAlertas) {
                            BadgedBox(badge = {
                                if (alertasHoy > 0) Badge(containerColor = ColorMalo) { Text("$alertasHoy") }
                            }) {
                                Icon(
                                    if (alertasHoy > 0) Icons.Rounded.NotificationsActive else Icons.Rounded.Notifications,
                                    contentDescription = "Alertas: $alertasHoy hoy",
                                    tint = Color.White,
                                )
                            }
                        }
                    },
                ) {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ChipEstadoConexion(ui.conexion)
                        Spacer(Modifier.width(10.dp))
                        if (ui.actualizado > 0) {
                            Text(
                                "Actualizado ${horaDe(ui.actualizado)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.75f),
                            )
                        }
                    }
                }
            }

            if (ui.conexion == EstadoConexion.SIN_CONEXION) {
                item { TarjetaSinConexion(ui.dispositivo.ip, alReintentar = vm::refrescar) }
            }

            item {
                AnimatedVisibility(
                    visible = ui.superaLimite,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    BannerAlerta(
                        titulo = "Opiniones negativas sobre el límite",
                        texto = "${e.porcentaje(Opinion.MALO)} % de votos \"Malo\" hoy (límite ${ui.config.umbralPorcentaje} %). Toca para ver qué pasó.",
                        onClick = alVerAlertas,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }

            item {
                Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TarjetaMetrica(
                        titulo = "Votos de hoy",
                        valor = "${e.total}",
                        icono = Icons.Rounded.HowToVote,
                        color = AzulSensi,
                        detalle = if (e.accesible > 0) "${e.accesible} con botón accesible" else null,
                        modifier = Modifier.weight(1f),
                    )
                    TarjetaMetrica(
                        titulo = "Satisfacción",
                        valor = if (e.total == 0) "—" else "${e.satisfaccion} %",
                        icono = Icons.Rounded.SentimentSatisfied,
                        color = when {
                            e.total == 0 -> AzulSensi
                            e.satisfaccion >= 70 -> ColorBueno
                            e.satisfaccion >= 50 -> ColorRegular
                            else -> ColorMalo
                        },
                        detalle = when {
                            e.total == 0 -> null
                            e.satisfaccion >= 70 -> "Clientes contentos"
                            e.satisfaccion >= 50 -> "Se puede mejorar"
                            else -> "Requiere atención"
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                TarjetaSeccion(
                    "Resultados de hoy",
                    icono = Icons.Rounded.PieChart,
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GraficoDona(
                            malo = e.malo,
                            regular = e.regular,
                            bueno = e.bueno,
                            textoCentro = "${e.total}",
                            subtextoCentro = "votos",
                            tamano = 112.dp,
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Opinion.entries.reversed().forEach { op ->
                                FilaOpinion(op, e.cantidad(op), e.porcentaje(op))
                            }
                        }
                    }
                }
            }

            item {
                TarjetaSeccion(
                    "Votos por hora",
                    icono = Icons.Rounded.Schedule,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    accion = { TextButton(onClick = alVerReportes) { Text("Reportes") } },
                ) {
                    GraficoBarrasApiladas(barrasDeHoy(e.porHora), descripcion = "Votos de hoy por hora")
                }
            }

            item {
                TarjetaSeccion(
                    "Actividad reciente",
                    icono = Icons.Rounded.Timeline,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    accion = { TextButton(onClick = alVerVotos) { Text("Ver todo") } },
                ) {
                    if (e.ultimos.isEmpty()) {
                        Text(
                            "Aún no llegan votos hoy.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    e.ultimos.take(3).forEachIndexed { i, voto ->
                        if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        FilaVotoCompacta(voto)
                    }
                }
            }

            item {
                TarjetaSeccion(
                    "Tu SensiTouch",
                    icono = Icons.Rounded.Router,
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    Dato("Botón accesible", if (e.modoAccesible) "Activo" else "Apagado")
                    Dato("Votos con botón accesible", "${e.accesible}")
                    Dato("Voz", if (e.voz) "Activada" else "En silencio")
                    Dato("Señal WiFi", if (ui.dispositivo.demo) "Simulada" else calidadSenal(e.rssi))
                    Dato("Dirección", if (ui.dispositivo.demo) "Modo demostración" else ui.dispositivo.ip)
                }
            }
        }
    }
}

@Composable
private fun TarjetaSinConexion(ip: String, alReintentar: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("No hay conexión con el SensiTouch", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Revisa que esté encendido y en la misma red ($ip). Se muestran los últimos datos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(onClick = alReintentar) { Text("Reintentar") }
        }
    }
}

@Composable
private fun Dato(nombre: String, valor: String) {
    Row(Modifier.padding(vertical = 5.dp)) {
        Text(nombre, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(valor, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun FilaVotoCompacta(voto: Voto, modifier: Modifier = Modifier) {
    val color = colorOpinion(voto.opinion)
    Row(modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(36.dp)
                .background(color.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(iconoOpinion(voto.opinion), contentDescription = null, tint = color)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(voto.opinion.etiqueta, style = MaterialTheme.typography.titleSmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    iconoOrigen(voto.porBoton),
                    contentDescription = null,
                    tint = if (voto.porBoton) ColorAccesible else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    if (voto.porBoton) "Botón accesible" else "Sensor del túnel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(haceCuanto(voto), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
