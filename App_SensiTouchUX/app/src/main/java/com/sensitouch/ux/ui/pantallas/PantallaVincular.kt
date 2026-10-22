package com.sensitouch.ux.ui.pantallas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sensitouch.ux.data.EstadoDispositivo
import com.sensitouch.ux.ui.AppViewModel
import com.sensitouch.ux.ui.calidadSenal
import com.sensitouch.ux.ui.componentes.EncabezadoGradiente
import com.sensitouch.ux.ui.componentes.TarjetaSeccion
import com.sensitouch.ux.ui.theme.ColorBueno
import com.sensitouch.ux.ui.theme.ColorRegular

@Composable
fun PantallaVincular(vm: AppViewModel, alTerminar: () -> Unit) {
    var ip by rememberSaveable { mutableStateOf("") }
    var probando by remember { mutableStateOf(false) }
    var resultado by remember { mutableStateOf<Result<EstadoDispositivo>?>(null) }
    val foco = LocalFocusManager.current

    val probar = {
        foco.clearFocus()
        probando = true
        resultado = null
        vm.probarConexion(ip) { probando = false; resultado = it }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .navigationBarsPadding(),
    ) {
        EncabezadoGradiente(
            titulo = "Vincular dispositivo",
            subtitulo = "Conecta tu SensiTouch por WiFi",
        )
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            TarjetaSeccion("Antes de empezar", icono = Icons.Rounded.Router) {
                Paso(1, "Enciende el SensiTouch con el interruptor.")
                Paso(2, "Conecta este celular a la misma red WiFi que el SensiTouch (o comparte datos desde el celular).")
                Paso(3, "Escribe la IP que aparece en la pantalla del dispositivo al encender.")
            }

            OutlinedTextField(
                value = ip,
                onValueChange = { ip = it.trim(); resultado = null },
                label = { Text("IP del SensiTouch") },
                placeholder = { Text("Ejemplo: 192.168.1.50") },
                leadingIcon = { Icon(Icons.Rounded.Wifi, contentDescription = null) },
                supportingText = { Text("La ves en la pantalla OLED, debajo de \"Conectado\"") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { if (ip.isNotBlank()) probar() }),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )

            FilledTonalButton(
                onClick = probar,
                enabled = ip.isNotBlank() && !probando,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
            ) {
                if (probando) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Buscando el SensiTouch…")
                } else {
                    Text("Probar conexión")
                }
            }

            AnimatedVisibility(visible = resultado != null) {
                val r = resultado
                if (r != null) {
                    r.fold(
                        onSuccess = { estado -> ResultadoExito(estado) },
                        onFailure = { ResultadoError() },
                    )
                }
            }

            Button(
                onClick = { vm.vincular(ip, demo = false, alTerminar = alTerminar) },
                enabled = resultado?.isSuccess == true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) { Text("Vincular e ir al panel", style = MaterialTheme.typography.titleMedium) }

            OutlinedButton(
                onClick = { vm.vincular("demo", demo = true, alTerminar = alTerminar) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Rounded.Science, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Probar sin dispositivo (modo demo)")
            }
            Text(
                "El modo demo genera votos de ejemplo para conocer la app o presentarla sin el prototipo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Paso(numero: Int, texto: String) {
    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .size(26.dp)
                .background(MaterialTheme.colorScheme.secondary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("$numero", color = MaterialTheme.colorScheme.onSecondary, style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.width(12.dp))
        Text(texto, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun ResultadoExito(estado: EstadoDispositivo) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ColorBueno.copy(alpha = 0.10f)),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = ColorBueno)
                Spacer(Modifier.width(8.dp))
                Text("¡SensiTouch encontrado!", style = MaterialTheme.typography.titleMedium)
            }
            Revision("Conexión WiFi", true, calidadSenal(estado.rssi))
            Revision("Pantalla OLED", estado.oled, if (estado.oled) "Funcionando" else "No detectada")
            Revision("Voz (DFPlayer Mini)", estado.dfplayer, if (estado.dfplayer) "Lista" else "No detectada: revisa la microSD y los cables")
            Revision("Votos registrados hoy", true, "${estado.total}")
        }
    }
}

@Composable
private fun Revision(nombre: String, ok: Boolean, detalle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
            contentDescription = if (ok) "Correcto" else "Revisar",
            tint = if (ok) ColorBueno else ColorRegular,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(nombre, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(detalle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ResultadoError() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.ErrorOutline, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("No se pudo conectar", style = MaterialTheme.typography.titleMedium)
            }
            Text("• Revisa que la IP sea la misma que muestra la pantalla.", style = MaterialTheme.typography.bodyMedium)
            Text("• El celular y el SensiTouch deben estar en la misma red WiFi.", style = MaterialTheme.typography.bodyMedium)
            Text("• Si usas datos del celular, activa el punto de acceso antes de encender el dispositivo.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
