package cl.duoc.casajojo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal val Cara = Color(0xFFFFFBF0)
internal val CardLight = Color(0xFFFFFFFF)
internal val Ink = Color(0xFF18352A)
internal val Mut = Color(0xFF5F6F66)
internal val Line = Color(0xFFD9E6D8)
internal val Ac = Color(0xFF008A55)
internal val Gold = Color(0xFFFFB300)
internal val Bad = Color(0xFFC53030)

internal val LightColors = lightColorScheme(
    primary = Ac,
    onPrimary = Color.Black,
    secondary = Gold,
    onSecondary = Color(0xFF3D2A00),
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
    onPrimary = Color.Black,
    secondary = Gold,
    onSecondary = Color(0xFF3D2A00),
    background = Color(0xFF10251C),
    surface = Color(0xFF18352A),
    onSurface = Color(0xFFFFF8E7),
    onSurfaceVariant = Color(0xFFB7C9B9),
    outline = Color(0xFF3E604D),
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
