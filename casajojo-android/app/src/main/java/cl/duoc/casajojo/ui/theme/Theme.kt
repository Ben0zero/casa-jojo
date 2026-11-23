package cl.duoc.casajojo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Paleta original de Casa JoJo recuperada: verde esmeralda, crema cálida y
 * dorado. Se conservan los nombres históricos (Cara, Ac, Gold, Ink…) para que
 * el cambio siga siendo puramente visual y no obligue a tocar lógica.
 *
 * Los tonos derivados antes llamados "Rojo…" ahora son verdes: el nombre se
 * renombró en las pantallas para que el tema quede coherente.
 */
internal val Cara = Color(0xFFFFFBF0)
internal val CardLight = Color(0xFFFFFFFF)
internal val Ink = Color(0xFF18352A)
internal val Mut = Color(0xFF5F6F66)
internal val Line = Color(0xFFD9E6D8)
internal val Ac = Color(0xFF008A55)
internal val Gold = Color(0xFFFFB300)
internal val Bad = Color(0xFFC53030)

/** Tonos extra usados por las tarjetas, los degradados y los estados. */
internal val VerdeOscuro = Color(0xFF006B41)
internal val VerdeSuave = Color(0xFFDFF3E4)
internal val FondoNoche = Color(0xFF06140E)
internal val Amarillo = Gold
internal val VerdeListo = Color(0xFF008A55)
internal val FotoFondo = Color(0xFFE8F5EA)
internal val FotoTexto = Color(0xFF3F6B52)

internal val LightColors = lightColorScheme(
    primary = Ac,
    onPrimary = Color.White,
    primaryContainer = VerdeSuave,
    onPrimaryContainer = VerdeOscuro,
    secondary = Gold,
    onSecondary = Color(0xFF3D2A00),
    secondaryContainer = Color(0xFFFFF2CC),
    onSecondaryContainer = Color(0xFF4A3600),
    background = Cara,
    onBackground = Ink,
    surface = CardLight,
    onSurface = Ink,
    surfaceVariant = Color(0xFFFFF2CC),
    onSurfaceVariant = Mut,
    outline = Line,
    error = Bad,
)

internal val DarkColors = darkColorScheme(
    primary = Ac,
    onPrimary = Color.White,
    primaryContainer = VerdeOscuro,
    onPrimaryContainer = VerdeSuave,
    secondary = Gold,
    onSecondary = Color(0xFF3D2A00),
    secondaryContainer = Color(0xFF214435),
    onSecondaryContainer = Color(0xFFFFF2CC),
    background = Color(0xFF10251C),
    onBackground = Color(0xFFFFF8E7),
    surface = Color(0xFF18352A),
    onSurface = Color(0xFFFFF8E7),
    surfaceVariant = Color(0xFF214435),
    onSurfaceVariant = Color(0xFFB7C9B9),
    outline = Color(0xFF3E604D),
    error = Color(0xFFFFB4AB),
)

/** Tipografía con más peso: títulos grandes y números muy visibles. */
internal val AppTypography = Typography(
    displayLarge = androidx.compose.ui.text.TextStyle(
        fontWeight = FontWeight.Black,
        fontSize = 42.sp,
        lineHeight = 46.sp,
    ),
    headlineLarge = androidx.compose.ui.text.TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        lineHeight = 37.sp,
    ),
    headlineMedium = androidx.compose.ui.text.TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 25.sp,
        lineHeight = 30.sp,
    ),
    titleLarge = androidx.compose.ui.text.TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 25.sp,
    ),
    titleMedium = androidx.compose.ui.text.TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
    ),
    bodyLarge = androidx.compose.ui.text.TextStyle(fontSize = 16.sp),
    bodyMedium = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
    labelLarge = androidx.compose.ui.text.TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
    ),
)

/** Esquinas más generosas: se siente más "app de comida" y menos formulario. */
internal val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun CasaJojoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
