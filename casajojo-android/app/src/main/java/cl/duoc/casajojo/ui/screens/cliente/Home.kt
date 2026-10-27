package cl.duoc.casajojo.ui.screens.cliente

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duoc.casajojo.ui.components.Boton
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
    onComenzar: () -> Unit,
    onMiPedido: () -> Unit,
    onPerfil: () -> Unit,
    onCerrarSesion: () -> Unit,
) {
    // NUEVO (fase 1): datos del usuario logueado para el saludo.
    val usuario by vm.usuario.collectAsState()

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
            // NUEVO (fase 1): saludo con el primer nombre y acceso a cerrar sesión.
            // (En la fase 2 "Cerrar sesión" pasa a la pantalla Perfil.)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Hola, ${usuario?.nombre?.substringBefore(' ') ?: ""}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "Mi perfil",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onPerfil)
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                )
                Text(
                    "Cerrar sesión",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onCerrarSesion)
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                )
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
            Boton("Comenzar mi pedido", onClick = onComenzar)
            // Solo se ofrece "Ver mi pedido" si ya existe al menos un pedido guardado.
            if (ultimoPedido != null) {
                Spacer(Modifier.height(10.dp))
                Boton(
                    "Ver mi pedido",
                    onClick = onMiPedido,
                    estilo = "fantasma",
                )
            }
        }
    }
}
