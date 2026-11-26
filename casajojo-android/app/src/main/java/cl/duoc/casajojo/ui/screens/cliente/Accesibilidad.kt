package cl.duoc.casajojo.ui.screens.cliente

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duoc.casajojo.domain.model.TemaApp
import cl.duoc.casajojo.ui.components.Boton
import cl.duoc.casajojo.ui.components.TarjetaBloque
import cl.duoc.casajojo.ui.components.Titulo
import cl.duoc.casajojo.ui.components.TituloBloque
import cl.duoc.casajojo.ui.components.Volver
import cl.duoc.casajojo.ui.layouts.Pantalla
import cl.duoc.casajojo.ui.viewmodels.AccesibilidadViewModel

/**
 * Accesibilidad: tema, tamaño de texto y reducir animaciones.
 * Cada cambio se guarda en DataStore (por cuenta) y se aplica al instante en toda la app,
 * porque CasaJojoTheme observa el mismo AccesibilidadViewModel.
 */
@Composable
fun ViewAccesibilidad(
    vm: AccesibilidadViewModel,
    onVolver: () -> Unit,
    barra: @Composable () -> Unit,
) {
    val opciones by vm.opciones.collectAsState()

    Pantalla(bar = barra) {
        Column(Modifier.fillMaxWidth()) {
            Volver(onVolver)
            Spacer(Modifier.height(18.dp))
            Titulo("Accesibilidad", "Ajusta la app a tu gusto. Se aplica al instante.")

            // ---------- Tema (selección única: RadioButton + selectableGroup para TalkBack) ----------
            TarjetaBloque {
                TituloBloque("🌗", "Tema")
                Column(Modifier.selectableGroup()) {
                    TemaApp.entries.forEach { tema ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = opciones.tema == tema,
                                    onClick = { vm.cambiarTema(tema) },
                                    role = Role.RadioButton,
                                )
                                .defaultMinSize(minHeight = 48.dp),
                        ) {
                            RadioButton(selected = opciones.tema == tema, onClick = null)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(tema.etiqueta, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                if (tema == TemaApp.SISTEMA) {
                                    Text(
                                        "Sigue el modo claro u oscuro del teléfono",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ---------- Interruptores ----------
            TarjetaBloque {
                TituloBloque("🔠", "Texto y movimiento")
                FilaInterruptor(
                    titulo = "Texto grande",
                    detalle = "Agranda todos los textos de la app (×1,15)",
                    activo = opciones.textoGrande,
                    onCambio = vm::cambiarTextoGrande,
                )
                FilaInterruptor(
                    titulo = "Reducir animaciones",
                    detalle = "Quita las transiciones entre pantallas y los efectos de movimiento",
                    activo = opciones.reducirAnimaciones,
                    onCambio = vm::cambiarReducirAnimaciones,
                )
            }

            // ---------- Vista previa con la configuración elegida ----------
            TarjetaBloque {
                TituloBloque("👀", "Vista previa")
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Pollo asado con arroz", fontWeight = FontWeight.Black, fontSize = 17.sp)
                        Text(
                            "Así se verán los textos de la app con tu configuración.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        Boton("Comenzar mi pedido", onClick = {}, estilo = "acento")
                    }
                }
            }
        }
    }
}

/** Fila completa tocable (≥ 48 dp) con un Switch; TalkBack la anuncia como interruptor. */
@Composable
private fun FilaInterruptor(titulo: String, detalle: String, activo: Boolean, onCambio: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = activo, onValueChange = onCambio, role = Role.Switch)
            .defaultMinSize(minHeight = 56.dp)
            .padding(vertical = 6.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(titulo, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(detalle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(10.dp))
        Switch(checked = activo, onCheckedChange = null)
    }
}
