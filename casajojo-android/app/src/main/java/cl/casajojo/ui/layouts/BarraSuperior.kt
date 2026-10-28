package cl.duoc.casajojo.ui.layouts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Barra superior con el logo y un acceso rápido al carrito (si tiene platos).
 * Es estructura de pantalla, por eso vive en ui/layouts y no en ui/components.
 */
@Composable
fun BarraSuperior(
    cartCount: Int,
    onCartClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("La Casa ", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("JoJo", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
        }
        // ANIMACIÓN: el acceso al carrito aparece/desaparece con un
        // despliegue horizontal en vez de saltar de golpe.
        // CORRECCIÓN (P2): antes estaba dentro de un if (cartCount > 0), así que
        // AnimatedVisibility entraba ya visible y salía sin animación. Ahora es
        // AnimatedVisibility quien decide cuándo mostrarlo.
        AnimatedVisibility(
            visible = cartCount > 0,
            enter = fadeIn(tween(200)) + expandHorizontally(tween(250)),
            exit = fadeOut(tween(150)) + shrinkHorizontally(tween(200)),
        ) {
            Surface(
                onClick = onCartClick,
                shape = RoundedCornerShape(99.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp, MaterialTheme.colorScheme.outline
                ),
                color = Color.Transparent,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        " Mi pedido ($cartCount)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}
