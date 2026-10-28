package cl.duoc.casajojo.ui.screens.cliente

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duoc.casajojo.ui.components.Boton
import cl.duoc.casajojo.ui.components.EstiloBoton
import cl.duoc.casajojo.ui.components.Titulo
import cl.duoc.casajojo.ui.components.TotalAnimado
import cl.duoc.casajojo.ui.components.Volver
import cl.duoc.casajojo.utils.money
import cl.duoc.casajojo.ui.layouts.Pantalla
import cl.duoc.casajojo.ui.viewmodels.ClienteViewModel

/**
 * PASO 3 — Carrito.
 * Lista los platos que el cliente ha armado (vm.cart). Desde aquí puede:
 * - quitar un plato,
 * - volver a Proteína para agregar otro plato,
 * - continuar a Retiro y pago (solo si hay al menos un plato).
 */
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

            // CORRECCIÓN: si el carrito está vacío (por ejemplo, porque se quitaron
            // todos los platos) mostramos un mensaje en vez de una lista en blanco.
            if (vm.cart.isEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Todavía no has agregado platos. Elige una proteína para empezar.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 16.sp,
                )
            }

            // forEachIndexed nos da la posición "i" de cada plato; la usamos para
            // saber cuál quitar, porque dos platos podrían ser idénticos.
            vm.cart.forEachIndexed { i, plato ->
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
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
                            // CORRECCIÓN: "Quitar" era solo texto y no hacía nada.
                            // Ahora llama a vm.quitarPlato(i); como cart es un
                            // mutableStateOf, la lista y el total se redibujan solos.
                            Text(
                                "Quitar",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { vm.quitarPlato(i) }
                                    .padding(vertical = 4.dp, horizontal = 2.dp),
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
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                // ANIMACIÓN: el total cuenta hasta el nuevo valor al agregar/quitar platos.
                TotalAnimado(valor = vm.total)
            }
            Spacer(Modifier.height(20.dp))
            Boton(
                if (vm.cart.isEmpty()) "Elegir proteína" else "Agregar otro plato",
                onClick = onAgregarOtro,
                estilo = EstiloBoton.Fantasma,
            )
            Spacer(Modifier.height(10.dp))
            // CORRECCIÓN: no se puede continuar a Retiro con el carrito vacío.
            // (El ViewModel también lo valida, por si se llega por otro camino.)
            Boton(
                "Continuar",
                onClick = onContinuar,
                estilo = EstiloBoton.Acento,
                enabled = vm.cart.isNotEmpty(),
            )
        }
    }
}