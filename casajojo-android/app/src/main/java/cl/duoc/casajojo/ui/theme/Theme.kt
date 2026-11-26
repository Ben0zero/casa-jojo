package cl.duoc.casajojo.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import cl.duoc.casajojo.domain.model.OpcionesAccesibilidad
import cl.duoc.casajojo.domain.model.TemaApp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
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
    // Contenedores M3 (diálogos, menús, campos): sin esto salían en lavanda por defecto.
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFBFDF9),
    surfaceContainer = Color(0xFFF5FAF4),
    surfaceContainerHigh = Color(0xFFF0F7EF),
    surfaceContainerHighest = Color(0xFFE8F2E7),
    surfaceTint = Ac,
    inverseSurface = Ink,
    inverseOnSurface = Color(0xFFFFF8E7),
    inversePrimary = Color(0xFF5FD69A),
)

internal val DarkColors = darkColorScheme(
    // Verde más claro que Ac: sobre los fondos oscuros, Ac quedaba bajo 3:1 de contraste
    // en textos, enlaces y botones de texto. Los fondos verdes siguen usando Ac.
    primary = Color(0xFF5FD69A),
    onPrimary = Color(0xFF00391F),
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
    errorContainer = Color(0xFF5C1A16),
    onErrorContainer = Color(0xFFFFDAD6),
    surfaceContainerLowest = Color(0xFF0C1D15),
    surfaceContainerLow = Color(0xFF142D22),
    surfaceContainer = Color(0xFF18352A),
    surfaceContainerHigh = Color(0xFF1E3D30),
    surfaceContainerHighest = Color(0xFF254737),
    surfaceTint = Ac,
    inverseSurface = Color(0xFFFFF8E7),
    inverseOnSurface = Ink,
    inversePrimary = Ac,
)

/**
 * Corte de línea en español: si una palabra larga ("acompañamientos") no cabe, se separa
 * en sílabas con guion en vez de partirse en cualquier letra. Text usa bodyLarge como base.
 */
private val CorteEspanol = androidx.compose.ui.text.TextStyle(
    hyphens = Hyphens.Auto,
    lineBreak = LineBreak.Paragraph,
    localeList = LocaleList("es-CL"),
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
    bodyLarge = CorteEspanol.copy(fontSize = 16.sp),
    bodyMedium = CorteEspanol.copy(fontSize = 14.sp),
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

/**
 * Accesibilidad: true si la app se está dibujando en oscuro. Lo usan las pantallas en vez
 * de isSystemInDarkTheme(), porque la persona puede forzar Claro u Oscuro.
 */
val LocalTemaOscuro = staticCompositionLocalOf { false }

/** Accesibilidad: true si la persona pidió reducir animaciones. */
val LocalReducirAnimaciones = staticCompositionLocalOf { false }

/** Atajo para preguntar el tema actual desde cualquier composable. */
@Composable
@ReadOnlyComposable
fun esTemaOscuro(): Boolean = LocalTemaOscuro.current

/** Atajo: duración de una animación, 0 si la persona pidió reducirlas. */
@Composable
@ReadOnlyComposable
fun duracionAnimacion(ms: Int): Int = if (LocalReducirAnimaciones.current) 0 else ms

/**
 * Tema de la app con las opciones de accesibilidad de la cuenta:
 * - Tema Sistema / Claro / Oscuro → elige la paleta y los íconos de la barra de estado.
 * - Texto grande → multiplica fontScale por 1.15: TODO texto en sp crece, en toda la app.
 * - Reducir animaciones → se expone en LocalReducirAnimaciones (NavHost, totales, listas).
 */
@Composable
fun CasaJojoTheme(
    opciones: OpcionesAccesibilidad = OpcionesAccesibilidad(),
    content: @Composable () -> Unit,
) {
    val oscuro = when (opciones.tema) {
        TemaApp.SISTEMA -> isSystemInDarkTheme()
        TemaApp.CLARO -> false
        TemaApp.OSCURO -> true
    }
    // Íconos de la barra de estado y de navegación legibles sobre el fondo elegido.
    val vista = LocalView.current
    if (!vista.isInEditMode) {
        SideEffect {
            val ventana = (vista.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(ventana, vista).apply {
                isAppearanceLightStatusBars = !oscuro
                isAppearanceLightNavigationBars = !oscuro
            }
        }
    }
    val densidad = LocalDensity.current
    CompositionLocalProvider(
        LocalTemaOscuro provides oscuro,
        LocalReducirAnimaciones provides opciones.reducirAnimaciones,
        LocalDensity provides Density(densidad.density, densidad.fontScale * opciones.escalaTexto),
    ) {
        MaterialTheme(
            colorScheme = if (oscuro) DarkColors else LightColors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
