package cl.duoc.casajojo.ui.cliente

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duoc.casajojo.data.entity.PedidoWithPlatos
import cl.duoc.casajojo.ui.components.BarraSuperior
import cl.duoc.casajojo.ui.components.Boton
import cl.duoc.casajojo.ui.components.Pill
import cl.duoc.casajojo.ui.components.Titulo
import cl.duoc.casajojo.ui.components.Volver
import cl.duoc.casajojo.ui.formato.colorEstadoPago
import cl.duoc.casajojo.ui.formato.colorEstadoPedido
import cl.duoc.casajojo.ui.formato.money
import cl.duoc.casajojo.ui.formato.textoEstadoPago
import cl.duoc.casajojo.ui.formato.textoEstadoPedido

@Composable
fun ViewMiPedido(
    vm: ClienteViewModel,
    onVolver: () -> Unit,
    onCart: () -> Unit,
    onEmpezar: () -> Unit,
) {
    val pedido = vm.ultimoPedido.value
    Column(Modifier.fillMaxWidth()) {
        BarraSuperior(cartCount = vm.cart.size, onCartClick = onCart)
        Pantalla {
            Column {
                Volver(onVolver)
                Titulo("Mi pedido")
                if (pedido == null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Aún no tienes pedidos registrados en este dispositivo.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 16.sp,
                    )
                    Spacer(Modifier.height(20.dp))
                    Boton("Hacer un pedido", onClick = onEmpezar, estilo = "acento")
                } else {
                    TarjetaPedido(pedido)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Estado: ${textoEstadoPedido(pedido.pedido.estadoPedido)} · Pago: ${textoEstadoPago(pedido.pedido.estadoPago)}. " +
                            "Puedes consultar esta pantalla para seguir tu pedido.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun TarjetaPedido(datos: PedidoWithPlatos) {
    val p = datos.pedido
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("#${p.numero}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.weight(1f))
                Pill(textoEstadoPedido(p.estadoPedido), colorEstadoPedido(p.estadoPedido))
                Pill(textoEstadoPago(p.estadoPago), colorEstadoPago(p.estadoPago))
            }
            Spacer(Modifier.height(8.dp))
            Text("A nombre de ${p.nombre}", fontSize = 15.sp)
            Text(
                "Retiro a las ${p.horaRetiro} h · ${p.formaPago}",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(10.dp))
            datos.platos.forEach { plato ->
                Text(
                    "• ${plato.nombreProteina}" +
                        if (plato.solo) " (solo proteína)" else " — ${plato.acompanamientos.joinToString(" · ")}",
                    fontSize = 14.sp,
                )
            }
            if (p.indicaciones.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Indicaciones: ${p.indicaciones}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End) {
                Text(p.total.money(), fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        }
    }
}