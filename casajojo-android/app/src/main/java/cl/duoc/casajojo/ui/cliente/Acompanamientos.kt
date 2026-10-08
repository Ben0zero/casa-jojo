package cl.duoc.casajojo.ui.cliente

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.duoc.casajojo.ui.components.Boton
import cl.duoc.casajojo.ui.components.ErrorEcho
import cl.duoc.casajojo.ui.components.OptionCard
import cl.duoc.casajojo.ui.components.PasoBar
import cl.duoc.casajojo.ui.components.Titulo
import cl.duoc.casajojo.ui.components.Volver
import cl.duoc.casajojo.ui.formato.money

@Composable
fun ViewAcompanamientos(
    vm: ClienteViewModel,
    onVolver: () -> Unit,
    onAgregar: () -> Unit,
) {
    ErrorEcho(vm.error) { vm.limpiarError() }
    val acompanamientos by vm.acompanamientos.collectAsState()
    val disponibles = acompanamientos.filter { it.disponible }
    val nombreProteina = vm.proteinaSeleccionada?.nombre ?: "tu proteína"
    Pantalla(
        bar = {
            Boton(
                texto = "Agregar al pedido  ·  ${vm.proteinaSeleccionada?.precio?.money() ?: ""}",
                onClick = onAgregar,
                estilo = "acento",
            )
        },
    ) {
        Column {
            Volver(onVolver)
            PasoBar(2)
            Titulo(
                "Elige tus acompañamientos",
                "Llevas: $nombreProteina",
            )

            disponibles.chunked(2).forEach { fila ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    fila.forEach { a ->
                        OptionCard(
                            texto = a.nombre,
                            estilo = "check",
                            marcado = vm.acompanamientosSel.contains(a.nombre),
                            onClick = { vm.toggleAcompanamiento(a.nombre) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (fila.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}