package cl.duoc.casajojo.ui.layouts

import androidx.compose.foundation.background
import cl.duoc.casajojo.ui.theme.esTemaOscuro
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cl.duoc.casajojo.ui.theme.Ac
import cl.duoc.casajojo.ui.theme.FondoNoche
import cl.duoc.casajojo.ui.theme.Gold

/**
 * Plantilla común de todas las pantallas del cliente.
 * - Arriba, contenido con scroll sobre un degradado suave: crema en claro y verde casi negro en oscuro.
 * - Abajo (opcional), una barra fija elevada con el botón principal.
 *
 * La firma no cambia: ninguna pantalla ni ViewModel necesita saber que el
 * contenedor ahora tiene sombra, esquinas redondas y un degradado.
 */
@Composable
fun Pantalla(
    bar: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val claro = !esTemaOscuro()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        if (claro) Color(0xFFE4F3E8) else FondoNoche,
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
    ) {
        // Filete verde/dorado: sello visual Casa JoJo en cada pantalla.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(Brush.horizontalGradient(listOf(Ac, Gold))),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 150.dp),
        ) {
            Column { content() }
        }
        if (bar != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 16.dp,
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .padding(top = 10.dp, bottom = 16.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .height(4.dp)
                            .width(42.dp)
                            .background(
                                MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(99.dp),
                            )
                            .align(Alignment.CenterHorizontally),
                    )
                    Box(modifier = Modifier.padding(top = 10.dp)) {
                        bar()
                    }
                }
            }
        }
    }
}
