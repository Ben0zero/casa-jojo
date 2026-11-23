package cl.duoc.casajojo.ui.screens.cliente

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import cl.duoc.casajojo.ui.components.FotoPlaceholder
import cl.duoc.casajojo.ui.components.PanelTotal
import cl.duoc.casajojo.ui.components.Titulo
import cl.duoc.casajojo.ui.components.Volver
import cl.duoc.casajojo.utils.money
import cl.duoc.casajojo.ui.layouts.Pantalla
import cl.duoc.casajojo.ui.theme.Ac
import cl.duoc.casajojo.ui.viewmodels.ClienteViewModel
import kotlin.math.round

private fun Double.decimal(): String = (round(this * 10) / 10).toString()

/**
 * PASO 3 — Carrito. Misma lógica de siempre: quitar platos, agregar otro
 * y continuar solo si hay al menos un plato. Ahora con look de ticket.
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
            Spacer(Modifier.height(16.dp))
            Titulo("Tu pedido", "Revisa lo que llevas antes de continuar")
            Spacer(Modifier.height(18.dp))

            if (vm.cart.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "Todavía no has agregado platos. Elige una proteína para empezar.",
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(18.dp),
                    )
                }
            }

            vm.cart.forEachIndexed { i, plato ->
                val estado = vm.nutricionPorPlato[plato]
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(18.dp),
                    shadowElevation = 3.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                ) {
                    Column(Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Box(Modifier.size(66.dp)) {
                                FotoPlaceholder(alto = 66.dp, etiqueta = "", redondeo = 13.dp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(plato.nombreProteina, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(
                                    if (plato.solo) {
                                        "Solo la proteína"
                                    } else {
                                        plato.acompanamientos.joinToString(" · ")
                                    },
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        "Quitar",
                                        color = Ac,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .padding(top = 8.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { vm.quitarPlato(i) }
                                            .padding(vertical = 4.dp, horizontal = 2.dp),
                                    )
                                    Text(
                                        if (estado?.expandido == true) {
                                            "Ocultar información nutricional"
                                        } else {
                                            "Ver información nutricional"
                                        },
                                        color = Ac,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .padding(top = 8.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable {
                                                if (estado?.expandido == true) {
                                                    vm.ocultarNutricion(plato)
                                                } else {
                                                    vm.mostrarNutricion(plato)
                                                }
                                            }
                                            .padding(vertical = 4.dp, horizontal = 2.dp),
                                    )
                                }
                            }
                            Text(
                                plato.precio.money(),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                        if (estado?.expandido == true) {
                            Spacer(Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    when {
                                        estado.cargando -> Text(
                                            "Consultando…",
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            fontSize = 13.sp,
                                        )
                                        estado.error != null -> {
                                            Text(
                                                estado.error,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                fontSize = 13.sp,
                                            )
                                            Text(
                                                "Reintentar",
                                                color = Ac,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .clickable { vm.reintentarNutricion(plato) }
                                                    .padding(vertical = 4.dp),
                                            )
                                        }
                                        estado.datos != null -> {
                                            estado.datos.items.forEach { item ->
                                                Text(
                                                    item.ingrediente,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                )
                                                Text(
                                                    "${item.kcal.toInt()} kcal · " +
                                                        "Proteína ${item.proteinas.decimal()} g · " +
                                                        "Carbohidrato ${item.carbohidratos.decimal()} g · " +
                                                        "Grasa ${item.grasas.decimal()} g",
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    fontSize = 12.sp,
                                                    modifier = Modifier.padding(bottom = 6.dp),
                                                )
                                            }
                                            Text(
                                                "Total: ${estado.datos.total.kcal.toInt()} kcal · " +
                                                    "Proteína ${estado.datos.total.proteinas.decimal()} g · " +
                                                    "Carbohidrato ${estado.datos.total.carbohidratos.decimal()} g · " +
                                                    "Grasa ${estado.datos.total.grasas.decimal()} g",
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                            Text(
                                                "Valores por 100 g · Fuente: Open Food Facts · Referencia aproximada",
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                fontSize = 11.sp,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            PanelTotal(valor = vm.total, etiqueta = "Total del pedido")
            Spacer(Modifier.height(20.dp))
            Boton(
                if (vm.cart.isEmpty()) "Elegir proteína" else "Agregar otro plato",
                onClick = onAgregarOtro,
                estilo = "fantasma",
            )
            Spacer(Modifier.height(10.dp))
            Boton(
                "Continuar",
                onClick = onContinuar,
                estilo = "acento",
                enabled = vm.cart.isNotEmpty(),
            )
        }
    }
}
