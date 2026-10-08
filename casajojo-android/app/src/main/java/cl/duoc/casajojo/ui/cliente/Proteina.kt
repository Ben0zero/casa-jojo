package cl.duoc.casajojo.ui.cliente

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.duoc.casajojo.domain.ReglasPedido
import cl.duoc.casajojo.ui.components.Boton
import cl.duoc.casajojo.ui.components.ErrorEcho
import cl.duoc.casajojo.ui.components.OptionCard
import cl.duoc.casajojo.ui.components.PasoBar
import cl.duoc.casajojo.ui.components.Titulo
import cl.duoc.casajojo.ui.components.Volver
import cl.duoc.casajojo.ui.formato.money

@Composable
fun ViewProteina(
    vm: ClienteViewModel,
    onVolver: () -> Unit,
    onIrAcom: () -> Unit,
    onIrCarrito: () -> Unit,
) {
    ErrorEcho(vm.error) { vm.limpiarError() }
    val proteinas by vm.proteinas.collectAsState()
    Pantalla(
        bar = {
            if (vm.cart.isNotEmpty()) {
                Boton(
                    texto = "Ver mi pedido (${vm.cart.size})  ·  ${vm.total.money()}",
                    onClick = onIrCarrito,
                    estilo = "acento",
                )
            }
        },
    ) {
        Column {
            Volver(onVolver)
            PasoBar(1)
            Titulo(
                "Elige tu proteína",
                "El precio incluye tus acompañamientos.",
            )

            proteinas.forEach { p ->
                OptionCard(
                    texto = p.nombre,
                    precio = if (p.disponible) p.precio.money() else "Agotado",
                    deshabilitado = !p.disponible,
                    marcado = false,
                    onClick = {
                        val elegirSolo = vm.solo
                        vm.elegirProteina(p)
                        if (p.disponible && elegirSolo) onIrCarrito()
                        else if (p.disponible) onIrAcom()
                    },
                )
            }

            Spacer(Modifier.height(12.dp))
            OptionCard(
                texto = "Solo la proteína, sin acompañamientos",
                precio = ReglasPedido.PRECIO_SOLO_PROTEINA.money(),
                estilo = "check",
                marcado = vm.solo,
                onClick = { vm.toggleSolo() },
            )
        }
    }
}