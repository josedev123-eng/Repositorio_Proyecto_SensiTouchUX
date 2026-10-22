package com.sensitouch.ux.ui.pantallas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sensitouch.ux.alertas.EvaluadorAlertas
import com.sensitouch.ux.data.Alerta
import com.sensitouch.ux.data.Opinion
import com.sensitouch.ux.data.ResumenDia
import com.sensitouch.ux.reportes.DatosReporte
import com.sensitouch.ux.reportes.GeneradorPdf
import com.sensitouch.ux.ui.UiEstado
import com.sensitouch.ux.ui.barrasDeHoy
import com.sensitouch.ux.ui.componentes.BarraApilada
import com.sensitouch.ux.ui.componentes.EncabezadoGradiente
import com.sensitouch.ux.ui.componentes.FilaOpinion
import com.sensitouch.ux.ui.componentes.GraficoBarrasApiladas
import com.sensitouch.ux.ui.componentes.TarjetaMetrica
import com.sensitouch.ux.ui.componentes.TarjetaSeccion
import com.sensitouch.ux.ui.etiquetaDia
import com.sensitouch.ux.ui.fechaHoraCorta
import com.sensitouch.ux.ui.fechaLarga
import com.sensitouch.ux.ui.nombreDia
import com.sensitouch.ux.ui.theme.AzulSensi
import com.sensitouch.ux.ui.theme.ColorBueno
import com.sensitouch.ux.ui.theme.ColorMalo
import com.sensitouch.ux.ui.ultimosDias
import com.sensitouch.ux.ui.AppViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class Periodo(val etiqueta: String, val dias: Int) {
    DIA("Día", 1),
    SEMANA("Semana", 7),
    MES("Mes", 30),
}

private data class DatosVista(
    val descripcion: String,
    val malo: Int,
    val regular: Int,
    val bueno: Int,
    val barras: List<BarraApilada>,
    val hallazgos: List<String>,
    val alertas: List<Alerta>,
) {
    val total: Int get() = malo + regular + bueno
    fun porcentaje(n: Int): Int = if (total == 0) 0 else (n * 100f / total).roundToInt()
    val satisfaccion: Int get() = if (total == 0) 0 else ((bueno * 100f + regular * 50f) / total).roundToInt()
    fun cantidad(o: Opinion) = when (o) {
        Opinion.MALO -> malo
        Opinion.REGULAR -> regular
        Opinion.BUENO -> bueno
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaReportes(vm: AppViewModel) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var periodo by rememberSaveable { mutableStateOf(Periodo.DIA) }
    var exportando by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val alcance = rememberCoroutineScope()

    val datos = remember(periodo, ui.estado, ui.historial, ui.alertas) { calcular(periodo, ui) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        EncabezadoGradiente(titulo = "Reportes", subtitulo = datos.descripcion)

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                Periodo.entries.forEachIndexed { i, p ->
                    SegmentedButton(
                        selected = periodo == p,
                        onClick = { periodo = p },
                        shape = SegmentedButtonDefaults.itemShape(index = i, count = Periodo.entries.size),
                    ) { Text(p.etiqueta) }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TarjetaMetrica("Votos", "${datos.total}", Icons.Rounded.BarChart, AzulSensi, Modifier.weight(1f))
                TarjetaMetrica(
                    "Satisfacción",
                    if (datos.total == 0) "—" else "${datos.satisfaccion} %",
                    Icons.Rounded.Insights,
                    ColorBueno,
                    Modifier.weight(1f),
                )
                TarjetaMetrica(
                    "Malo",
                    if (datos.total == 0) "—" else "${datos.porcentaje(datos.malo)} %",
                    Icons.Rounded.WarningAmber,
                    ColorMalo,
                    Modifier.weight(1f),
                )
            }

            TarjetaSeccion("Distribución", icono = Icons.Rounded.PieChart) {
                Opinion.entries.reversed().forEach { op ->
                    FilaOpinion(op, datos.cantidad(op), datos.porcentaje(datos.cantidad(op)))
                }
            }

            TarjetaSeccion(
                when (periodo) {
                    Periodo.DIA -> "Votos por hora"
                    Periodo.SEMANA -> "Últimos 7 días"
                    Periodo.MES -> "Últimos 30 días"
                },
                icono = Icons.Rounded.BarChart,
            ) {
                GraficoBarrasApiladas(datos.barras, descripcion = "Gráfico de votos del periodo")
            }

            TarjetaSeccion("Lo que dicen los datos", icono = Icons.Rounded.Lightbulb) {
                if (datos.hallazgos.isEmpty()) {
                    Text(
                        "Cuando haya más votos verás aquí las horas y días con más quejas.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                datos.hallazgos.forEach { h ->
                    Row(Modifier.padding(vertical = 4.dp)) {
                        Text("•", modifier = Modifier.width(16.dp), color = MaterialTheme.colorScheme.tertiary)
                        Text(h, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            TarjetaSeccion("Alertas del periodo (${datos.alertas.size})", icono = Icons.Rounded.WarningAmber) {
                if (datos.alertas.isEmpty()) {
                    Text(
                        "Sin alertas. Las opiniones negativas se mantuvieron bajo tu límite.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                datos.alertas.take(5).forEach { a ->
                    Column(Modifier.padding(vertical = 4.dp)) {
                        Text(a.titulo, style = MaterialTheme.typography.titleSmall)
                        Text(fechaHoraCorta(a.fechaHora), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Button(
                onClick = {
                    exportando = true
                    alcance.launch {
                        val archivo = GeneradorPdf.crear(
                            context,
                            DatosReporte(
                                negocio = ui.negocio?.nombre ?: "Mi negocio",
                                periodo = datos.descripcion,
                                malo = datos.malo,
                                regular = datos.regular,
                                bueno = datos.bueno,
                                barras = datos.barras,
                                hallazgos = datos.hallazgos,
                                alertas = datos.alertas.map { "${fechaHoraCorta(it.fechaHora)} — ${it.titulo}" },
                            ),
                        )
                        exportando = false
                        GeneradorPdf.compartir(context, archivo)
                    }
                },
                enabled = !exportando,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                if (exportando) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Rounded.PictureAsPdf, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Exportar reporte en PDF")
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ------------------------------------------------------------------
// Calculos del reporte
// ------------------------------------------------------------------

private fun satisfaccion(d: ResumenDia): Int =
    if (d.total == 0) 0 else ((d.bueno * 100f + d.regular * 50f) / d.total).roundToInt()

private fun pctMalo(d: ResumenDia): Int = if (d.total == 0) 0 else d.malo * 100 / d.total

private fun fechaDe(milis: Long): LocalDate = Instant.ofEpochMilli(milis).atZone(ZoneId.systemDefault()).toLocalDate()

private fun calcular(periodo: Periodo, ui: UiEstado): DatosVista {
    val hoy = LocalDate.now()
    val desde = hoy.minusDays((periodo.dias - 1).toLong())
    val alertas = ui.alertas.filter { !fechaDe(it.fechaHora).isBefore(desde) }
    val e = ui.estado

    // El dia de hoy siempre sale del dispositivo (lo mas actual)
    val historial = if (ui.hayDatos) {
        ui.historial.filter { it.fecha != hoy.toString() } + ResumenDia(hoy.toString(), e.malo, e.regular, e.bueno)
    } else ui.historial

    if (periodo == Periodo.DIA) {
        val hallazgos = buildList {
            if (e.total > 0) {
                e.porHora.maxByOrNull { it.total }?.takeIf { it.total > 0 }?.let {
                    add("La hora con más clientes fue entre las ${EvaluadorAlertas.rango(it.hora)} (${it.total} votos).")
                }
                e.horaConMasQuejas?.let { h ->
                    val v = e.porHora.first { it.hora == h }
                    add("La mayoría de quejas llegó entre las ${EvaluadorAlertas.rango(h)} (${v.malo} votos \"Malo\"). Revisa la atención en ese horario.")
                }
                val ayer = historial.firstOrNull { it.fecha == hoy.minusDays(1).toString() }
                if (ayer != null && ayer.total > 0) {
                    val dif = e.satisfaccion - satisfaccion(ayer)
                    add(
                        when {
                            dif > 0 -> "La satisfacción subió $dif puntos frente a ayer."
                            dif < 0 -> "La satisfacción bajó ${abs(dif)} puntos frente a ayer."
                            else -> "La satisfacción está igual que ayer."
                        },
                    )
                }
                if (e.accesible > 0) add("${e.accesible} persona(s) usaron el botón accesible para opinar.")
            }
        }
        return DatosVista(
            descripcion = "Hoy, ${fechaLarga(hoy).replaceFirstChar { it.lowercase() }}",
            malo = e.malo,
            regular = e.regular,
            bueno = e.bueno,
            barras = barrasDeHoy(e.porHora),
            hallazgos = hallazgos,
            alertas = alertas,
        )
    }

    val dias = ultimosDias(historial, periodo.dias, hoy)
    val anteriores = ultimosDias(historial, periodo.dias, hoy.minusDays(periodo.dias.toLong()))
    val malo = dias.sumOf { it.malo }
    val regular = dias.sumOf { it.regular }
    val bueno = dias.sumOf { it.bueno }
    val total = malo + regular + bueno
    val conVotos = dias.filter { it.total > 0 }
    val nombre = if (periodo == Periodo.SEMANA) "semana" else "mes"

    val hallazgos = buildList {
        if (conVotos.isNotEmpty()) {
            add("En promedio llegan ${total / conVotos.size} opiniones por día.")
            conVotos.maxByOrNull { satisfaccion(it) }?.let {
                add("El mejor día fue el ${nombreDia(it.fecha)} con ${satisfaccion(it)} % de satisfacción.")
            }
            conVotos.maxByOrNull { pctMalo(it) }?.takeIf { it.malo > 0 }?.let {
                add("El día con más quejas fue el ${nombreDia(it.fecha)}: ${pctMalo(it)} % de votos \"Malo\".")
            }
            val totalAnterior = anteriores.sumOf { it.total }
            if (totalAnterior > 0 && total > 0) {
                val satActual = ((bueno * 100f + regular * 50f) / total).roundToInt()
                val satAnterior = ((anteriores.sumOf { it.bueno } * 100f + anteriores.sumOf { it.regular } * 50f) / totalAnterior).roundToInt()
                val dif = satActual - satAnterior
                add(
                    when {
                        dif > 0 -> "La satisfacción subió $dif puntos frente a la $nombre anterior."
                        dif < 0 -> "La satisfacción bajó ${abs(dif)} puntos frente a la $nombre anterior."
                        else -> "La satisfacción se mantuvo igual que la $nombre anterior."
                    }.replace("la mes", "el mes"),
                )
            }
        }
    }

    return DatosVista(
        descripcion = if (periodo == Periodo.SEMANA) "Últimos 7 días" else "Últimos 30 días",
        malo = malo,
        regular = regular,
        bueno = bueno,
        barras = dias.map { BarraApilada(etiquetaDia(it.fecha, corta = periodo == Periodo.SEMANA), it.malo, it.regular, it.bueno) },
        hallazgos = hallazgos,
        alertas = alertas,
    )
}
