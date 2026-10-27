package cl.duoc.casajojo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal val Cara = Color(0xFFFFF6D8)
internal val CardLight = Color(0xFFFFFFFF)
internal val Ink = Color(0xFF4A2B12)
internal val Mut = Color(0xFF806448)
internal val Line = Color(0xFFE4CFA1)
internal val Ac = Color(0xFFFF9F1C)
internal val Emerald = Color(0xFF079B5A)
internal val Bad = Color(0xFFC53030)

internal val LightColors = lightColorScheme(
    primary = Ac,
    onPrimary = Color(0xFF4A2B12),
    secondary = Emerald,
    onSecondary = Color.White,
    background = Cara,
    onBackground = Ink,
    surface = CardLight,
    onSurface = Ink,
    surfaceVariant = Color(0xFFFFE9B0),
    onSurfaceVariant = Mut,
    outline = Line,
    error = Bad,
)

internal val DarkColors = darkColorScheme(
    primary = Ac,
    onPrimary = Color(0xFF4A2B12),
    secondary = Emerald,
    onSecondary = Color.White,
    background = Color(0xFF2B1B0F),
    surface = Color(0xFF4A2B12),
    onSurface = Color(0xFFFFF6D8),
    onSurfaceVariant = Color(0xFFE4CFA1),
    outline = Color(0xFF806448),
)

@Composable
fun CasaJojoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
