package com.sensitouch.ux.ui.pantallas

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Accessible
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.HowToVote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sensitouch.ux.data.Opinion
import com.sensitouch.ux.data.Voto
import com.sensitouch.ux.ui.AppViewModel
import com.sensitouch.ux.ui.componentes.EncabezadoGradiente
import com.sensitouch.ux.ui.componentes.EstadoVacio
import com.sensitouch.ux.ui.componentes.colorOpinion
import com.sensitouch.ux.ui.componentes.iconoOpinion
import com.sensitouch.ux.ui.haceCuanto
import com.sensitouch.ux.ui.theme.ColorAccesible

private enum class Filtro(val etiqueta: String) { TODOS("Todos"), MALO("Malo"), REGULAR("Regular"), BUENO("Bueno"), ACCESIBLE("Accesible") }

@Composable
fun PantallaVotos(vm: AppViewModel) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val e = ui.estado
    var filtro by rememberSaveable { mutableStateOf(Filtro.TODOS) }

    val lista = e.ultimos.filter {
        when (filtro) {
            Filtro.TODOS -> true
            Filtro.MALO -> it.opinion == Opinion.MALO
            Filtro.REGULAR -> it.opinion == Opinion.REGULAR
            Filtro.BUENO -> it.opinion == Opinion.BUENO
            Filtro.ACCESIBLE -> it.porBoton
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            EncabezadoGradiente(
                titulo = "Votos en tiempo real",
                subtitulo = "Se actualiza solo cada 3 segundos",
                acciones = { ChipEnVivo() },
            ) {
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Opinion.entries.forEach { op ->
                        ContadorBlanco(op, e.cantidad(op), Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            UltimoVoto(e.ultimos.firstOrNull(), Modifier.padding(16.dp))
        }

        item {
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Filtro.entries.forEach { f ->
                    val seleccionado = filtro == f
                    FilterChip(
                        selected = seleccionado,
                        onClick = { filtro = f },
                        label = { Text(f.etiqueta) },
                        leadingIcon = if (seleccionado) {
                            { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                        } else null,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        if (lista.isEmpty()) {
            item {
                EstadoVacio(
                    icono = Icons.Rounded.HowToVote,
                    titulo = if (filtro == Filtro.TODOS) "Esperando votos" else "Sin votos de este tipo",
                    texto = "Aquí aparecerá cada voto apenas alguien pase la mano por el SensiTouch.",
                )
            }
        }

        items(lista, key = { it.id }) { voto ->
            FilaVotoCompacta(
                voto,
                Modifier
                    .animateItem()
                    .padding(horizontal = 20.dp),
            )
        }

        item {
            Text(
                "Se muestran los últimos 20 votos que guarda el dispositivo.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
        }
    }
}

@Composable
private fun ChipEnVivo() {
    val pulso = rememberInfiniteTransition(label = "vivo")
    val alfa by pulso.animateFloat(1f, 0.2f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "alfa")
    Surface(shape = CircleShape, color = Color(0xFFE5484D)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(Color.White.copy(alpha = alfa), CircleShape),
            )
            Spacer(Modifier.width(6.dp))
            Text("EN VIVO", style = MaterialTheme.typography.labelMedium, color = Color.White)
        }
    }
}

@Composable
private fun ContadorBlanco(opinion: Opinion, cantidad: Int, modifier: Modifier = Modifier) {
    Surface(modifier, shape = MaterialTheme.shapes.medium, color = Color.White.copy(alpha = 0.14f), contentColor = Color.White) {
        Column(Modifier.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(iconoOpinion(opinion), contentDescription = null, tint = colorOpinion(opinion))
            Text("$cantidad", style = MaterialTheme.typography.titleLarge)
            Text(opinion.etiqueta, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun UltimoVoto(voto: Voto?, modifier: Modifier = Modifier) {
    Card(
        modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = voto?.let { colorOpinion(it.opinion).copy(alpha = 0.12f) }
                ?: MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        AnimatedContent(
            targetState = voto,
            transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.92f)) togetherWith fadeOut() },
            contentKey = { it?.id },
            label = "ultimoVoto",
        ) { v ->
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                if (v == null) {
                    Text("Todavía no hay votos hoy", style = MaterialTheme.typography.titleMedium)
                } else {
                    val color = colorOpinion(v.opinion)
                    Box(
                        Modifier
                            .size(64.dp)
                            .background(color, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(iconoOpinion(v.opinion), contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Último voto", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(v.opinion.etiqueta, style = MaterialTheme.typography.headlineMedium, color = color)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (v.porBoton) {
                                Icon(Icons.Rounded.Accessible, contentDescription = null, tint = ColorAccesible, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                            }
                            Text(
                                "${haceCuanto(v)} · ${if (v.porBoton) "botón accesible" else "sensor del túnel"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
