package com.sensitouch.ux.ui.pantallas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sensitouch.ux.alertas.EvaluadorAlertas
import com.sensitouch.ux.data.Alerta
import com.sensitouch.ux.data.Opinion
import com.sensitouch.ux.data.TipoAlerta
import com.sensitouch.ux.ui.AppViewModel
import com.sensitouch.ux.ui.componentes.BarraApilada
import com.sensitouch.ux.ui.componentes.EncabezadoGradiente
import com.sensitouch.ux.ui.componentes.EstadoVacio
import com.sensitouch.ux.ui.componentes.GraficoBarrasApiladas
import com.sensitouch.ux.ui.componentes.TarjetaSeccion
import com.sensitouch.ux.ui.esDeHoy
import com.sensitouch.ux.ui.fechaHoraCorta
import com.sensitouch.ux.ui.theme.ColorMalo
import com.sensitouch.ux.ui.theme.ColorRegular

@Composable
fun PantallaAlertas(vm: AppViewModel, alVolver: () -> Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val e = ui.estado
    val deHoy = ui.alertas.filter { esDeHoy(it.fechaHora) }
    val anteriores = ui.alertas.filterNot { esDeHoy(it.fechaHora) }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            EncabezadoGradiente(
                titulo = "Alertas",
                subtitulo = if (deHoy.isEmpty()) "Sin alertas hoy" else "${deHoy.size} alerta(s) hoy",
                navegacion = {
                    IconButton(onClick = alVolver) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
            )
        }

        // Ayuda a encontrar el problema: en que hora se concentran las quejas de hoy
        item {
            TarjetaSeccion(
                "¿Dónde está el problema?",
                icono = Icons.Rounded.Search,
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                val horaPico = e.horaConMasQuejas
                Text(
                    when {
                        e.total == 0 -> "Aún no hay votos hoy."
                        horaPico == null -> "Hoy no hay opiniones \"Malo\". ¡Buen trabajo!"
                        else -> "Hoy van ${e.malo} quejas (${e.porcentaje(Opinion.MALO)} %). " +
                            "La mayoría llegó entre las ${EvaluadorAlertas.rango(horaPico)}: revisa qué pasó en ese turno."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (horaPico != null) {
                    Spacer(Modifier.size(12.dp))
                    val conVotos = e.porHora.filter { it.total > 0 }
                    val barras = (conVotos.minOf { it.hora }..conVotos.maxOf { it.hora }).map { h ->
                        val v = e.porHora.firstOrNull { it.hora == h }
                        BarraApilada("${h}h", v?.malo ?: 0, 0, 0)
                    }
                    GraficoBarrasApiladas(barras, alto = 120.dp, descripcion = "Quejas por hora")
                }
            }
        }

        if (ui.alertas.isEmpty()) {
            item {
                EstadoVacio(
                    icono = Icons.Rounded.NotificationsNone,
                    titulo = "Todo tranquilo",
                    texto = "Aquí verás cada vez que las opiniones negativas pasen tu límite o lleguen seguidas.",
                )
            }
        }

        if (deHoy.isNotEmpty()) {
            item { Subtitulo("Hoy") }
            items(deHoy, key = { it.id }) { TarjetaAlerta(it) }
        }
        if (anteriores.isNotEmpty()) {
            item { Subtitulo("Anteriores") }
            items(anteriores, key = { it.id }) { TarjetaAlerta(it) }
        }
    }
}

@Composable
private fun Subtitulo(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
    )
}

@Composable
private fun TarjetaAlerta(alerta: Alerta) {
    val (icono, color) = when (alerta.tipo) {
        TipoAlerta.RACHA -> Icons.Rounded.Bolt to ColorRegular
        TipoAlerta.DESCONECTADO -> Icons.Rounded.LinkOff to MaterialTheme.colorScheme.onSurfaceVariant
        else -> Icons.Rounded.WarningAmber to ColorMalo
    }
    Card(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Box(
                Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icono, contentDescription = null, tint = color)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(alerta.titulo, style = MaterialTheme.typography.titleSmall)
                Text(
                    fechaHoraCorta(alerta.fechaHora),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.size(6.dp))
                Text(alerta.detalle, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
