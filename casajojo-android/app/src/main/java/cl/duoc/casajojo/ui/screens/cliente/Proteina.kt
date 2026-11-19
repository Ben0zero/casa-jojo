package cl.duoc.casajojo.ui.screens.cliente

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duoc.casajojo.domain.usecase.ReglasPedido
import cl.duoc.casajojo.ui.components.Badge
import cl.duoc.casajojo.ui.components.BarraPedido
import cl.duoc.casajojo.ui.components.ErrorEcho
import cl.duoc.casajojo.ui.components.OptionCard
import cl.duoc.casajojo.ui.components.PasoBar
import cl.duoc.casajojo.ui.components.TarjetaMenu
import cl.duoc.casajojo.ui.components.Titulo
import cl.duoc.casajojo.ui.components.Volver
import cl.duoc.casajojo.utils.money
import cl.duoc.casajojo.ui.layouts.Pantalla
import cl.duoc.casajojo.ui.viewmodels.ClienteViewModel

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
                BarraPedido(
                    texto = "Ver mi pedido (${vm.cart.size})",
                    detalle = "Total ${vm.total.money()} · sigue armando tu pedido",
                    onClick = onIrCarrito,
                )
            }
        },
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Volver(onVolver)
                Spacer(Modifier.weight(1f))
                Badge("Paso 1 de 3")
            }
            Spacer(Modifier.height(14.dp))
            PasoBar(1)
            Titulo(
                "Elige tu proteína",
                "El precio incluye tus acompañamientos.",
            )
            Spacer(Modifier.height(18.dp))

            proteinas.forEachIndexed { i, p ->
                TarjetaMenu(
                    nombre = p.nombre,
                    precio = if (p.disponible) p.precio.money() else "Agotado",
                    disponible = p.disponible,
                    destacado = i == 0 && p.disponible,
                    onClick = {
                        val elegirSolo = vm.solo
                        vm.elegirProteina(p)
                        if (p.disponible && elegirSolo) onIrCarrito()
                        else if (p.disponible) onIrAcom()
                    },
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            Spacer(Modifier.height(22.dp))
            androidx.compose.material3.Text(
                "¿Prefieres ir directo al grano?",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
            )
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
