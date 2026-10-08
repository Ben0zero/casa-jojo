package cl.duoc.casajojo.ui.cliente

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duoc.casajojo.ui.components.Boton
import cl.duoc.casajojo.ui.components.Titulo
import cl.duoc.casajojo.ui.components.Volver
import cl.duoc.casajojo.ui.formato.money

@Composable
fun ViewCarrito(
    vm: ClienteViewModel,
    onVolver: () -> Unit,
    onAgregarOtro: () -> Unit,
    onContinuar: () -> Unit,
) {
    Pantalla {
        Column {
            Volver(onVolver)
            Titulo("Tu pedido")
            vm.cart.forEachIndexed { i, plato ->
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(plato.nombreProteina, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Text(
                                if (plato.solo) {
                                    "Solo la proteína"
                                } else {
                                    plato.acompanamientos.joinToString(" · ")
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp,
                            )
                            Text(
                                "Quitar",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                        Text(
                            plato.precio.money(),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
                Text("Total", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(vm.total.money(), fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            Spacer(Modifier.height(20.dp))
            Boton("Agregar otro plato", onClick = onAgregarOtro, estilo = "fantasma")
            Spacer(Modifier.height(10.dp))
            Boton("Continuar", onClick = onContinuar, estilo = "acento")
        }
    }
}