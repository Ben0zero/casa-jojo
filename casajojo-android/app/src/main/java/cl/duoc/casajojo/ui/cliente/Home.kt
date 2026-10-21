package cl.duoc.casajojo.ui.cliente

import androidx.compose.foundation.background
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duoc.casajojo.ui.components.Boton

/**
 * Plantilla común de todas las pantallas del cliente:
 * - Arriba, el contenido con scroll.
 * - Abajo (opcional), una barra fija con el botón principal ("bar").
 */
@Composable
fun Pantalla(
    bar: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 96.dp),
        ) {
            Column { content() }
        }
        if (bar != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 12.dp),
            ) {
                bar()
            }
        }
    }
}

@Composable
fun Regla() {
    Box(
        modifier = Modifier
            .padding(top = 12.dp, bottom = 18.dp)
            .background(
                MaterialTheme.colorScheme.primary,
                RoundedCornerShape(2.dp),
            )
            .height(3.dp)
            .fillMaxWidth(0.35f),
    )
}

@Composable
fun Nota(texto: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 0.dp, topEnd = 10.dp, bottomEnd = 10.dp, bottomStart = 0.dp),
        modifier = Modifier.padding(top = 20.dp),
    ) {
        Row {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .background(MaterialTheme.colorScheme.primary),
            )
            Text(
                texto,
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 14.dp),
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * PASO 0 — Portada.
 * Desde aquí el cliente empieza un pedido nuevo o revisa el último que hizo.
 */
@Composable
fun ViewHome(
    vm: ClienteViewModel,
    onComenzar: () -> Unit,
    onMiPedido: () -> Unit,
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