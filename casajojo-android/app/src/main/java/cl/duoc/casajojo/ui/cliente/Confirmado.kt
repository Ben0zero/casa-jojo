package cl.duoc.casajojo.ui.cliente

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duoc.casajojo.ui.components.Boton
import cl.duoc.casajojo.ui.components.Pill
import cl.duoc.casajojo.ui.formato.colorEstadoPago
import cl.duoc.casajojo.ui.formato.colorEstadoPedido
import cl.duoc.casajojo.ui.formato.money
import cl.duoc.casajojo.ui.formato.textoEstadoPago
import cl.duoc.casajojo.ui.formato.textoEstadoPedido

@Composable
fun ViewConfirmado(
    vm: ClienteViewModel,
    onConsultar: () -> Unit,
    onOtroPedido: () -> Unit,
) {
    val pedido = vm.ultimoPedido.value?.pedido
    Pantalla {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(top = 28.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            ) {
                Text("✓", color = Color.White, fontSize = 40.sp, lineHeight = 40.sp)
            }
            Spacer(Modifier.height(18.dp))
            Text("Pedido confirmado", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                "Tu número de pedido es",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 15.sp,
            )
            Text(
                "#${vm.ultimoNumero ?: "…"}",
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp),
            )

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Pill(
                            pedido?.estadoPedido?.let { textoEstadoPedido(it) } ?: "…",
                            pedido?.estadoPedido?.let { colorEstadoPedido(it) } ?: Color(0xFF6D6D67),
                        )
                        Pill(
                            pedido?.estadoPago?.let { textoEstadoPago(it) } ?: "…",
                            pedido?.estadoPago?.let { colorEstadoPago(it) } ?: Color(0xFF6D6D67),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Retiro a las ${pedido?.horaRetiro ?: "…"} h",
                        fontSize = 16.sp,
                    )
                    val platos = vm.ultimoPedido.value?.platos.orEmpty()
                    if (platos.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${platos.size} ${if (platos.size == 1) "plato" else "platos"} · ${pedido?.total?.money() ?: "…"}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Boton("Consultar estado de mi pedido", onClick = onConsultar, estilo = "acento")
            Spacer(Modifier.height(10.dp))
            Boton("Hacer otro pedido", onClick = onOtroPedido, estilo = "fantasma")
        }
    }
}