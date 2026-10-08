package cl.duoc.casajojo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal val Cara = Color(0xFFF6F5F2)
internal val CardLight = Color(0xFFFFFFFF)
internal val Ink = Color(0xFF161616)
internal val Mut = Color(0xFF6D6D67)
internal val Line = Color(0xFFE0DFD9)
internal val Ac = Color(0xFFF26A0F)
internal val Bad = Color(0xFFC53030)

internal val LightColors = lightColorScheme(
    primary = Ac,
    onPrimary = Color.White,
    background = Cara,
    onBackground = Ink,
    surface = CardLight,
    onSurface = Ink,
    surfaceVariant = Color(0xFFECEBE5),
    onSurfaceVariant = Mut,
    outline = Line,
    error = Bad,
)

internal val DarkColors = darkColorScheme(
    primary = Ac,
    background = Color(0xFF111111),
    surface = Color(0xFF1B1B1A),
    onSurface = Color(0xFFF1F1EC),
    onSurfaceVariant = Color(0xFF9C9C94),
    outline = Color(0xFF2F2F2C),
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