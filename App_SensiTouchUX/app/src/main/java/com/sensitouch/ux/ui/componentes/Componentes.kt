package com.sensitouch.ux.ui.componentes

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Accessible
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.SentimentNeutral
import androidx.compose.material.icons.rounded.SentimentVeryDissatisfied
import androidx.compose.material.icons.rounded.SentimentVerySatisfied
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sensitouch.ux.data.EstadoConexion
import com.sensitouch.ux.data.Opinion
import com.sensitouch.ux.ui.theme.AzulMarino
import com.sensitouch.ux.ui.theme.AzulSensi
import com.sensitouch.ux.ui.theme.ColorBueno
import com.sensitouch.ux.ui.theme.ColorMalo
import com.sensitouch.ux.ui.theme.ColorRegular

// ------------------------------------------------------------------
// Colores e iconos de cada opinion
// ------------------------------------------------------------------

fun colorOpinion(opinion: Opinion): Color = when (opinion) {
    Opinion.MALO -> ColorMalo
    Opinion.REGULAR -> ColorRegular
    Opinion.BUENO -> ColorBueno
}

fun iconoOpinion(opinion: Opinion): ImageVector = when (opinion) {
    Opinion.MALO -> Icons.Rounded.SentimentVeryDissatisfied
    Opinion.REGULAR -> Icons.Rounded.SentimentNeutral
    Opinion.BUENO -> Icons.Rounded.SentimentVerySatisfied
}

fun iconoOrigen(porBoton: Boolean): ImageVector =
    if (porBoton) Icons.Rounded.Accessible else Icons.Rounded.TouchApp

// ------------------------------------------------------------------
// Encabezado azul con degradado (igual al mockup)
// ------------------------------------------------------------------

@Composable
fun EncabezadoGradiente(
    titulo: String,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    navegacion: (@Composable () -> Unit)? = null,
    acciones: @Composable RowScope.() -> Unit = {},
    contenido: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(Brush.linearGradient(listOf(AzulMarino, AzulSensi)))
            .statusBarsPadding()
            .padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (navegacion != null) {
                navegacion()
                Spacer(Modifier.width(4.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    titulo,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitulo != null) {
                    Text(
                        subtitulo,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.82f),
                    )
                }
            }
            acciones()
        }
        Column(Modifier.padding(end = 8.dp)) { contenido() }
    }
}

// ------------------------------------------------------------------
// Tarjetas
// ------------------------------------------------------------------

@Composable
fun TarjetaSeccion(
    titulo: String,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null,
    accion: (@Composable () -> Unit)? = null,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icono != null) {
                    Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(titulo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                accion?.invoke()
            }
            Spacer(Modifier.height(14.dp))
            contenido()
        }
    }
}

@Composable
fun TarjetaMetrica(
    titulo: String,
    valor: String,
    icono: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    detalle: String? = null,
) {
    Card(
        modifier = modifier.semantics(mergeDescendants = true) {},
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(valor, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(titulo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (detalle != null) {
                Text(detalle, style = MaterialTheme.typography.labelSmall, color = color)
            }
        }
    }
}

/** Fila con icono, nombre, barra de progreso y cantidad de una opinion. */
@Composable
fun FilaOpinion(opinion: Opinion, cantidad: Int, porcentaje: Int, modifier: Modifier = Modifier) {
    val progreso by animateFloatAsState(porcentaje / 100f, label = "progreso")
    val color = colorOpinion(opinion)
    Row(
        modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "${opinion.etiqueta}: $cantidad votos, $porcentaje por ciento"
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(iconoOpinion(opinion), contentDescription = null, tint = color, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row {
                Text(opinion.etiqueta, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text(
                    "$cantidad · $porcentaje %",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progreso },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape),
                color = color,
                trackColor = color.copy(alpha = 0.15f),
                strokeCap = StrokeCap.Round,
            )
        }
    }
}

// ------------------------------------------------------------------
// Estado de conexion
// ------------------------------------------------------------------

@Composable
fun ChipEstadoConexion(conexion: EstadoConexion, modifier: Modifier = Modifier) {
    val (texto, color) = when (conexion) {
        EstadoConexion.CONECTADO -> "En línea" to Color(0xFF4ADE80)
        EstadoConexion.DEMO -> "Demo" to Color(0xFF67E8F9)
        EstadoConexion.CONECTANDO -> "Conectando…" to Color(0xFFFDE68A)
        EstadoConexion.SIN_CONEXION -> "Sin conexión" to Color(0xFFFCA5A5)
    }
    val pulso = rememberInfiniteTransition(label = "pulso")
    val alfa by pulso.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "alfa",
    )
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.16f),
        contentColor = Color.White,
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = if (conexion == EstadoConexion.SIN_CONEXION) 1f else alfa)),
            )
            Spacer(Modifier.width(6.dp))
            Text(texto, style = MaterialTheme.typography.labelMedium)
        }
    }
}

// ------------------------------------------------------------------
// Banner de alerta (rojo)
// ------------------------------------------------------------------

@Composable
fun BannerAlerta(titulo: String, texto: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = Color.White)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(texto, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null)
        }
    }
}

// ------------------------------------------------------------------
// Graficos
// ------------------------------------------------------------------

data class BarraApilada(val etiqueta: String, val malo: Int, val regular: Int, val bueno: Int) {
    val total: Int get() = malo + regular + bueno
}

/** Barras apiladas: verde abajo, amarillo al medio y rojo arriba. */
@Composable
fun GraficoBarrasApiladas(
    barras: List<BarraApilada>,
    modifier: Modifier = Modifier,
    alto: Dp = 180.dp,
    descripcion: String = "Gráfico de votos",
) {
    if (barras.isEmpty() || barras.all { it.total == 0 }) {
        EstadoVacio(
            icono = Icons.Rounded.TouchApp,
            titulo = "Aún no hay votos",
            texto = "Cuando alguien pase la mano por el SensiTouch verás el gráfico aquí.",
            modifier = modifier,
        )
        return
    }
    val medidor = rememberTextMeasurer()
    val estiloEtiqueta = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val colorGuia = MaterialTheme.colorScheme.outlineVariant
    val maximo = maxOf(1, barras.maxOf { it.total })
    val paso = when {
        barras.size > 20 -> 5
        barras.size > 12 -> 2
        else -> 1
    }
    Column(modifier) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(alto)
                .semantics { contentDescription = descripcion },
        ) {
            val altoEtiqueta = 20.dp.toPx()
            val altoUtil = size.height - altoEtiqueta
            val ancho = size.width / barras.size
            val anchoBarra = (ancho * 0.62f).coerceAtMost(26.dp.toPx())
            val radio = CornerRadius(5.dp.toPx())

            for (i in 0..3) {
                val y = altoUtil * i / 3f
                drawLine(colorGuia, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            }
            barras.forEachIndexed { i, barra ->
                val x = i * ancho + (ancho - anchoBarra) / 2f
                var base = altoUtil
                listOf(barra.bueno to ColorBueno, barra.regular to ColorRegular, barra.malo to ColorMalo)
                    .forEach { (valor, color) ->
                        if (valor > 0) {
                            val h = altoUtil * valor / maximo
                            drawRoundRect(color, Offset(x, base - h), Size(anchoBarra, maxOf(h - 1.5f, 1f)), radio)
                            base -= h
                        }
                    }
                if (i % paso == 0) {
                    val texto = medidor.measure(barra.etiqueta, estiloEtiqueta)
                    drawText(
                        texto,
                        topLeft = Offset(
                            (x + anchoBarra / 2f - texto.size.width / 2f).coerceIn(0f, maxOf(0f, size.width - texto.size.width)),
                            altoUtil + 4.dp.toPx(),
                        ),
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        LeyendaOpiniones()
    }
}

@Composable
fun LeyendaOpiniones(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)) {
        Opinion.entries.forEach { op ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(colorOpinion(op)),
                )
                Spacer(Modifier.width(6.dp))
                Text(op.etiqueta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Dona con el porcentaje de cada opinion y el indice de satisfaccion al centro. */
@Composable
fun GraficoDona(
    malo: Int,
    regular: Int,
    bueno: Int,
    textoCentro: String,
    subtextoCentro: String,
    modifier: Modifier = Modifier,
    tamano: Dp = 132.dp,
) {
    val total = malo + regular + bueno
    val animacion by animateFloatAsState(if (total > 0) 1f else 0f, tween(800), label = "dona")
    val pista = MaterialTheme.colorScheme.surfaceContainerHighest
    Box(modifier.size(tamano), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(tamano)) {
            val grosor = 16.dp.toPx()
            val estilo = Stroke(width = grosor, cap = StrokeCap.Butt)
            val inset = grosor / 2
            val area = Size(size.width - grosor, size.height - grosor)
            drawArc(pista, 0f, 360f, false, Offset(inset, inset), area, style = estilo)
            if (total > 0) {
                var inicio = -90f
                listOf(bueno to ColorBueno, regular to ColorRegular, malo to ColorMalo).forEach { (valor, color) ->
                    val barrido = 360f * valor / total * animacion
                    if (barrido > 0f) {
                        drawArc(color, inicio, barrido, false, Offset(inset, inset), area, style = estilo)
                        inicio += barrido
                    }
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(textoCentro, style = MaterialTheme.typography.headlineSmall)
            Text(subtextoCentro, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ------------------------------------------------------------------
// Estado vacio
// ------------------------------------------------------------------

@Composable
fun EstadoVacio(icono: ImageVector, titulo: String, texto: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
        }
        Spacer(Modifier.height(12.dp))
        Text(titulo, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            texto,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}
