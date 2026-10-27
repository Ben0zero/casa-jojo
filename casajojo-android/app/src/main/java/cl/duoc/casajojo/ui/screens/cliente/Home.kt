package cl.duoc.casajojo.ui.screens.cliente

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duoc.casajojo.data.local.entity.Usuario
import cl.duoc.casajojo.ui.components.Boton
import cl.duoc.casajojo.ui.components.EstiloBoton
import cl.duoc.casajojo.ui.components.Nota
import cl.duoc.casajojo.ui.components.Regla
import cl.duoc.casajojo.ui.layouts.Pantalla
import cl.duoc.casajojo.ui.viewmodels.ClienteViewModel

/**
 * PASO 0 — Portada.
 * Desde aquí el cliente empieza un pedido nuevo o revisa el último que hizo.
 */
@Composable
fun ViewHome(
    vm: ClienteViewModel,
    usuario: Usuario?,
    onComenzar: () -> Unit,
    onMiPedido: () -> Unit,
    onPerfil: () -> Unit,
    onHistorial: () -> Unit,
) {
    // CORRECCIÓN: antes se leía vm.ultimoPedido.value directamente.
    // El StateFlow del ViewModel usa SharingStarted.WhileSubscribed, es decir,
    // solo consulta Room mientras alguien lo está "escuchando". Leer .value no
    // cuenta como escuchar, así que el valor quedaba siempre en null y el botón
    // "Ver mi pedido" nunca aparecía.
    // collectAsState() se suscribe al Flow y además hace que la pantalla se
    // vuelva a dibujar (recomponga) cada vez que llega un valor nuevo.
    val ultimoPedido by vm.ultimoPedido.collectAsState()

    Pantalla {
        Column {
            // Saludo con el primer nombre y acceso al perfil.
            // CAMBIO (P3): "Cerrar sesión" se movió a la pantalla Perfil, y el
            // acceso al perfil ahora es un ícono en vez de un texto.
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Hola, ${usuario?.nombre?.substringBefore(' ') ?: ""}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                // NUEVO (RF-14): acceso al Historial, junto al perfil.
                IconButton(onClick = onHistorial) {
                    Icon(
                        Icons.Filled.History,
                        contentDescription = "Historial",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(onClick = onPerfil) {
                    Icon(
                        Icons.Filled.AccountCircle,
                        contentDescription = "Mi perfil",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Text("Arma tu menú", fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Regla()
            Text(
                "Elige tu proteína y tus acompañamientos. Lo dejamos listo en la puerta a la hora que indiques.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 17.sp,
            )
            Nota(
                "Pide antes de las 12 y retira sin esperar: tu almuerzo sale en menos de 10 minutos."
            )
            Spacer(Modifier.height(20.dp))
            Boton("Comenzar mi pedido", onClick = onComenzar, icono = Icons.Filled.Restaurant)
            // Solo se ofrece "Ver mis pedidos" si ya existe al menos un pedido guardado.
            if (ultimoPedido != null) {
                Spacer(Modifier.height(10.dp))
                Boton(
                    "Ver mis pedidos",
                    onClick = onMiPedido,
                    estilo = EstiloBoton.Fantasma,
                    icono = Icons.AutoMirrored.Filled.ReceiptLong,
                )
            }
        }
    }
}
