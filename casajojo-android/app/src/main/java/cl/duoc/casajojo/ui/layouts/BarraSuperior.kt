package cl.duoc.casajojo.ui.layouts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duoc.casajojo.R
import cl.duoc.casajojo.ui.theme.Gold

/**
 * Barra superior con el logo de La Casa JoJo y un acceso rápido al carrito.
 * Mantiene la misma firma para no tocar ninguna pantalla.
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
            Image(
                painter = painterResource(R.drawable.casa_jojo_logo),
                contentDescription = "Logo de La Casa JoJo",
                modifier = Modifier.size(42.dp),
                contentScale = ContentScale.Fit,
            )
            Column(modifier = Modifier.padding(start = 10.dp)) {
                Text(
                    "LA CASA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "JOJO",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
        if (cartCount > 0) {
            AnimatedVisibility(
                visible = cartCount > 0,
                enter = fadeIn(tween(200)) + expandHorizontally(tween(250)),
                exit = fadeOut(tween(150)) + shrinkHorizontally(tween(200)),
            ) {
                Surface(
                    onClick = onCartClick,
                    shape = RoundedCornerShape(99.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 3.dp,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, MaterialTheme.colorScheme.outline,
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Mi pedido",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                        )
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .background(Gold, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "$cartCount",
                                color = Color(0xFF3D2A00),
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}
