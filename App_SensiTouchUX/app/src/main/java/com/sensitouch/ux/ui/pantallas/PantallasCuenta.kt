package com.sensitouch.ux.ui.pantallas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Store
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.sensitouch.ux.ui.AppViewModel
import com.sensitouch.ux.ui.theme.AzulMarino
import com.sensitouch.ux.ui.theme.AzulSensi
import com.sensitouch.ux.ui.theme.Turquesa

// ------------------------------------------------------------------
// Fondo azul con el logo (comun a login y registro)
// ------------------------------------------------------------------

@Composable
private fun FondoCuenta(
    alVolver: (() -> Unit)? = null,
    contenido: @Composable () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(AzulMarino, AzulSensi))),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .systemBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (alVolver != null) {
                Box(Modifier.fillMaxWidth()) {
                    IconButton(onClick = alVolver) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                }
            } else {
                Spacer(Modifier.height(40.dp))
            }
            Surface(shape = CircleShape, color = Color.White, modifier = Modifier.size(84.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.TouchApp, contentDescription = null, tint = Turquesa, modifier = Modifier.size(46.dp))
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                buildAnnotatedString {
                    append("SensiTouch ")
                    withStyle(SpanStyle(color = Color(0xFF5ED8D3))) { append("UX") }
                },
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
            )
            Text(
                "Opiniones sin contacto, en tiempo real",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            ) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    contenido()
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CampoTexto(
    valor: String,
    alCambiar: (String) -> Unit,
    etiqueta: String,
    icono: ImageVector,
    tipoTeclado: KeyboardType = KeyboardType.Text,
    accion: ImeAction = ImeAction.Next,
    alTerminar: () -> Unit = {},
    esClave: Boolean = false,
    error: String? = null,
) {
    var verClave by rememberSaveable { mutableStateOf(false) }
    val foco = LocalFocusManager.current
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(etiqueta) },
        leadingIcon = { Icon(icono, contentDescription = null) },
        trailingIcon = if (esClave) {
            {
                IconButton(onClick = { verClave = !verClave }) {
                    Icon(
                        if (verClave) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = if (verClave) "Ocultar contraseña" else "Mostrar contraseña",
                    )
                }
            }
        } else null,
        visualTransformation = if (esClave && !verClave) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = tipoTeclado, imeAction = accion),
        keyboardActions = KeyboardActions(
            onNext = { foco.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) },
            onDone = { foco.clearFocus(); alTerminar() },
        ),
        isError = error != null,
        supportingText = if (error != null) { { Text(error) } } else null,
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun BotonPrincipal(texto: String, habilitado: Boolean, cargando: Boolean, alPresionar: () -> Unit) {
    Button(
        onClick = alPresionar,
        enabled = habilitado && !cargando,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = MaterialTheme.shapes.medium,
    ) {
        if (cargando) {
            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
        } else {
            Text(texto, style = MaterialTheme.typography.titleMedium)
        }
    }
}

// ------------------------------------------------------------------
// Login
// ------------------------------------------------------------------

@Composable
fun PantallaLogin(vm: AppViewModel, alEntrar: (String) -> Unit, alCrearCuenta: () -> Unit) {
    var correo by rememberSaveable { mutableStateOf("") }
    var clave by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var cargando by remember { mutableStateOf(false) }

    val entrar = {
        if (correo.isNotBlank() && clave.isNotBlank()) {
            cargando = true
            error = null
            vm.iniciarSesion(
                correo,
                clave,
                alError = { error = it; cargando = false },
                alEntrar = { ruta -> cargando = false; alEntrar(ruta) },
            )
        }
    }

    FondoCuenta {
        Text("Iniciar sesión", style = MaterialTheme.typography.titleLarge)
        Text(
            "Ingresa con la cuenta de tu negocio para ver las opiniones de tus clientes.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        CampoTexto(correo, { correo = it; error = null }, "Correo", Icons.Rounded.Email, KeyboardType.Email)
        CampoTexto(
            clave, { clave = it; error = null }, "Contraseña", Icons.Rounded.Lock, KeyboardType.Password,
            accion = ImeAction.Done, alTerminar = entrar, esClave = true,
        )
        if (error != null) {
            Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        BotonPrincipal("Ingresar", correo.isNotBlank() && clave.isNotBlank(), cargando, entrar)
        TextButton(onClick = alCrearCuenta, modifier = Modifier.fillMaxWidth()) {
            Text("¿Primera vez? Crea la cuenta de tu negocio")
        }
    }
}

// ------------------------------------------------------------------
// Registro
// ------------------------------------------------------------------

@Composable
fun PantallaRegistro(vm: AppViewModel, alVolver: () -> Unit, alTerminar: () -> Unit) {
    var nombre by rememberSaveable { mutableStateOf("") }
    var correo by rememberSaveable { mutableStateOf("") }
    var clave by rememberSaveable { mutableStateOf("") }
    var confirmar by rememberSaveable { mutableStateOf("") }
    var intento by remember { mutableStateOf(false) }
    var cargando by remember { mutableStateOf(false) }

    val errorCorreo = if (intento && !Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(correo.trim())) "Escribe un correo válido" else null
    val errorClave = if (intento && clave.length < 6) "Usa al menos 6 caracteres" else null
    val errorConfirmar = if (intento && confirmar != clave) "Las contraseñas no coinciden" else null
    val errorNombre = if (intento && nombre.isBlank()) "Escribe el nombre de tu negocio" else null

    val crear = {
        intento = true
        val valido = nombre.isNotBlank() && clave.length >= 6 && confirmar == clave &&
            Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(correo.trim())
        if (valido) {
            cargando = true
            vm.registrar(nombre, correo, clave) { cargando = false; alTerminar() }
        }
    }

    FondoCuenta(alVolver = alVolver) {
        Text("Crear cuenta de negocio", style = MaterialTheme.typography.titleLarge)
        Text(
            "La cuenta queda guardada en este celular. Después vincularás tu SensiTouch.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        CampoTexto(nombre, { nombre = it }, "Nombre del negocio", Icons.Rounded.Store, error = errorNombre)
        CampoTexto(correo, { correo = it }, "Correo", Icons.Rounded.Email, KeyboardType.Email, error = errorCorreo)
        CampoTexto(clave, { clave = it }, "Contraseña", Icons.Rounded.Lock, KeyboardType.Password, esClave = true, error = errorClave)
        CampoTexto(
            confirmar, { confirmar = it }, "Repite la contraseña", Icons.Rounded.Lock, KeyboardType.Password,
            accion = ImeAction.Done, alTerminar = crear, esClave = true, error = errorConfirmar,
        )
        BotonPrincipal("Crear cuenta", true, cargando, crear)
    }
}
