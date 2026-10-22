package com.sensitouch.ux.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val EsquemaClaro = lightColorScheme(
    primary = AzulMarino,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9E4F5),
    onPrimaryContainer = Color(0xFF0B1F3A),
    secondary = AzulSensi,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD4E6F7),
    onSecondaryContainer = Color(0xFF0A2E4F),
    tertiary = Turquesa,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC8F0EE),
    onTertiaryContainer = Color(0xFF00302F),
    error = ColorMalo,
    onError = Color.White,
    errorContainer = Color(0xFFFDE2E2),
    onErrorContainer = Color(0xFF5C0B0E),
    background = FondoClaro,
    onBackground = TextoPrincipal,
    surface = Color.White,
    onSurface = TextoPrincipal,
    surfaceVariant = Color(0xFFE6ECF4),
    onSurfaceVariant = TextoSecundario,
    outline = Color(0xFFC3CEDC),
    outlineVariant = Color(0xFFDDE4EE),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF8FAFD),
    surfaceContainer = Color(0xFFF0F4F9),
    surfaceContainerHigh = Color(0xFFEAF0F7),
    surfaceContainerHighest = Color(0xFFE3EAF3),
)

private val EsquemaOscuro = darkColorScheme(
    primary = Color(0xFFA9C7FF),
    onPrimary = Color(0xFF0B2A57),
    primaryContainer = Color(0xFF233F6E),
    onPrimaryContainer = Color(0xFFD9E4F5),
    secondary = Color(0xFF8CC4F5),
    onSecondary = Color(0xFF00324F),
    secondaryContainer = Color(0xFF1C4A70),
    onSecondaryContainer = Color(0xFFD4E6F7),
    tertiary = Color(0xFF5ED8D3),
    onTertiary = Color(0xFF003735),
    tertiaryContainer = Color(0xFF00504D),
    onTertiaryContainer = Color(0xFFC8F0EE),
    error = Color(0xFFFF8A8D),
    onError = Color(0xFF5C0B0E),
    errorContainer = Color(0xFF7A1E22),
    onErrorContainer = Color(0xFFFDE2E2),
    background = Color(0xFF0F1726),
    onBackground = Color(0xFFE2E8F2),
    surface = Color(0xFF162133),
    onSurface = Color(0xFFE2E8F2),
    surfaceVariant = Color(0xFF26324A),
    onSurfaceVariant = Color(0xFFAEBBD0),
    outline = Color(0xFF55637C),
    outlineVariant = Color(0xFF34415A),
    surfaceContainerLowest = Color(0xFF0B111D),
    surfaceContainerLow = Color(0xFF131D2E),
    surfaceContainer = Color(0xFF182337),
    surfaceContainerHigh = Color(0xFF1E2A40),
    surfaceContainerHighest = Color(0xFF26324A),
)

private val FormasSensi = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

@Composable
fun SensiTouchTheme(
    oscuro: Boolean = isSystemInDarkTheme(),
    contenido: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (oscuro) EsquemaOscuro else EsquemaClaro,
        typography = TipografiaSensi,
        shapes = FormasSensi,
        content = contenido,
    )
}
