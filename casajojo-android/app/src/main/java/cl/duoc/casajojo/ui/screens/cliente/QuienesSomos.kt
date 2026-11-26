package cl.duoc.casajojo.ui.screens.cliente

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duoc.casajojo.BuildConfig
import cl.duoc.casajojo.R
import cl.duoc.casajojo.ui.components.TarjetaBloque
import cl.duoc.casajojo.ui.components.TituloBloque
import cl.duoc.casajojo.ui.components.Volver
import cl.duoc.casajojo.ui.layouts.Pantalla

/** Equipo de desarrollo del proyecto. */
private val EQUIPO = listOf("Nicolás Arancibia", "Pablo Gómez", "Benjamin González")

/**
 * Quiénes somos: logo, descripción del local, equipo, asignatura y versión.
 * Los datos de contacto viven en res/values/strings.xml para editarlos sin tocar código.
 */
@Composable
fun ViewQuienesSomos(
    onVolver: () -> Unit,
    barra: @Composable () -> Unit,
) {
    Pantalla(bar = barra) {
        Column(Modifier.fillMaxWidth()) {
            Volver(onVolver)
            Spacer(Modifier.height(18.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = painterResource(R.drawable.casa_jojo_logo),
                    contentDescription = "Logo de La Casa JoJo",
                    modifier = Modifier.size(120.dp).clip(RoundedCornerShape(28.dp)),
                )
                Spacer(Modifier.height(12.dp))
                Text("La Casa JoJo", fontSize = 26.sp, fontWeight = FontWeight.Black)
                Text(
                    "Almuerzos a tu manera",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            TarjetaBloque {
                TituloBloque("🍽️", "Quiénes somos")
                Text(
                    stringResource(R.string.quienes_descripcion),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 10.dp),
                )
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.quienes_direccion), fontSize = 13.sp)
                Text(stringResource(R.string.quienes_telefono), fontSize = 13.sp)
                Text(stringResource(R.string.quienes_horario), fontSize = 13.sp)
            }

            TarjetaBloque {
                TituloBloque("👩‍💻", "Equipo de desarrollo")
                EQUIPO.forEach {
                    Text("• $it", fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
                }
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.quienes_asignatura), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(stringResource(R.string.quienes_institucion), fontSize = 13.sp)
            }

            Text(
                "Versión ${BuildConfig.VERSION_NAME}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            )
        }
    }
}
