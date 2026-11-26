package cl.duoc.casajojo.ui.screens.cliente

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duoc.casajojo.data.local.entity.Tarjeta
import cl.duoc.casajojo.domain.usecase.MarcaTarjeta
import cl.duoc.casajojo.ui.components.Badge
import cl.duoc.casajojo.ui.components.Boton
import cl.duoc.casajojo.ui.components.CampoTexto
import cl.duoc.casajojo.ui.components.FormatoNumeroTarjeta
import cl.duoc.casajojo.ui.components.FormatoVencimiento
import cl.duoc.casajojo.ui.components.TarjetaBloque
import cl.duoc.casajojo.ui.components.Titulo
import cl.duoc.casajojo.ui.components.TituloBloque
import cl.duoc.casajojo.ui.components.Volver
import cl.duoc.casajojo.ui.layouts.Pantalla
import cl.duoc.casajojo.ui.theme.Gold
import cl.duoc.casajojo.ui.theme.Ink
import cl.duoc.casajojo.ui.viewmodels.BilleteraViewModel

/**
 * Billetera (SIMULADA): tarjetas guardadas, predeterminada y formulario para agregar.
 * Nunca se muestra ni se guarda el número completo: solo "•••• 4242".
 */
@Composable
fun ViewBilletera(
    vm: BilleteraViewModel,
    onVolver: () -> Unit,
    barra: @Composable () -> Unit,
) {
    val tarjetas by vm.tarjetas.collectAsState()
    val estado by vm.estado.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(estado.mensaje) {
        estado.mensaje?.let {
            snackbar.showSnackbar(it)
            vm.limpiarMensaje()
        }
    }
    // Al salir de la pantalla se borra lo escrito: número y CVV no quedan en memoria.
    DisposableEffect(Unit) { onDispose { vm.cerrarFormulario() } }

    Box(Modifier.fillMaxSize()) {
        Pantalla(bar = barra) {
            Column(Modifier.fillMaxWidth()) {
                Volver(onVolver)
                Spacer(Modifier.height(18.dp))
                Titulo("Billetera", "Tus tarjetas para pagar en La Casa JoJo")
                Spacer(Modifier.height(14.dp))

                // Aviso visible: la billetera es una simulación.
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Gold.copy(alpha = 0.25f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("⚠️", fontSize = 18.sp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Simulación: no se realizan cobros reales",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                        )
                    }
                }

                if (tarjetas.isEmpty() && !estado.mostrandoFormulario) {
                    EstadoVacio()
                }

                tarjetas.forEach { t ->
                    TarjetaVisual(
                        tarjeta = t,
                        onPredeterminada = { vm.marcarPredeterminada(t) },
                        onEliminar = { vm.pedirEliminar(t) },
                    )
                }

                Spacer(Modifier.height(18.dp))
                if (estado.mostrandoFormulario) {
                    FormularioTarjeta(vm)
                } else {
                    Boton("➕  Agregar tarjeta", onClick = vm::abrirFormulario, estilo = "acento")
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }

    estado.porEliminar?.let { t ->
        AlertDialog(
            onDismissRequest = vm::cancelarEliminar,
            icon = { Text("🗑️", fontSize = 26.sp) },
            title = { Text("¿Eliminar ${t.etiqueta}?", fontWeight = FontWeight.Black) },
            text = { Text("La tarjeta se quitará de tu Billetera en este teléfono.") },
            confirmButton = {
                TextButton(onClick = vm::confirmarEliminar) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = vm::cancelarEliminar) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun EstadoVacio() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(top = 36.dp, bottom = 8.dp),
    ) {
        Text("💳", fontSize = 52.sp)
        Spacer(Modifier.height(10.dp))
        Text("Aún no tienes tarjetas guardadas", fontWeight = FontWeight.Black, fontSize = 17.sp)
        Text(
            "Agrega una para elegirla al pagar tu pedido.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Colores de la tarjeta según la marca (solo decorativo). */
private fun degradadoDe(marca: MarcaTarjeta): List<Color> = when (marca) {
    MarcaTarjeta.VISA -> listOf(Color(0xFF1A3A8F), Color(0xFF2F6BD8))
    MarcaTarjeta.MASTERCARD -> listOf(Color(0xFF3B1F1F), Color(0xFFD9542B))
    MarcaTarjeta.AMEX -> listOf(Color(0xFF0E5E6F), Color(0xFF2A9D8F))
    MarcaTarjeta.OTRA -> listOf(Ink, Color(0xFF3F5F50))
}

/** Tarjeta con diseño de plástico: marca, •••• últimos 4, titular y vencimiento. */
@Composable
private fun TarjetaVisual(tarjeta: Tarjeta, onPredeterminada: () -> Unit, onEliminar: () -> Unit) {
    val marca = MarcaTarjeta.desde(tarjeta.marca)
    Column(Modifier.padding(top = 16.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(degradadoDe(marca)))
                .padding(18.dp)
                // TalkBack lee la tarjeta completa en una frase, sin el número entero.
                .semantics {
                    contentDescription = "${tarjeta.etiqueta}, titular ${tarjeta.titular}, vence ${tarjeta.vencimiento}" +
                        if (tarjeta.predeterminada) ", predeterminada" else ""
                },
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        tarjeta.marca,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        modifier = Modifier.weight(1f),
                    )
                    if (tarjeta.predeterminada) Badge("★ Predeterminada", color = Gold, colorTexto = Ink)
                }
                if (tarjeta.alias.isNotBlank()) {
                    Text(tarjeta.alias, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                }
                Spacer(Modifier.height(22.dp))
                Text(
                    "••••  ••••  ••••  ${tarjeta.ultimos4}",
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                )
                Spacer(Modifier.height(18.dp))
                Row {
                    Column(Modifier.weight(1f)) {
                        Text("TITULAR", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                        Text(
                            tarjeta.titular,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("VENCE", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                        Text(tarjeta.vencimiento, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            if (!tarjeta.predeterminada) {
                TextButton(onClick = onPredeterminada, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Usar como predeterminada", fontWeight = FontWeight.Bold)
                }
            }
            TextButton(onClick = onEliminar, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Eliminar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Formulario "Agregar tarjeta": formato automático y error bajo cada campo. */
@Composable
private fun FormularioTarjeta(vm: BilleteraViewModel) {
    val estado by vm.estado.collectAsState()
    val f = estado.formulario
    TarjetaBloque(Modifier.padding(top = 0.dp)) {
        TituloBloque("💳", "Agregar tarjeta")
        CampoTexto(
            etiqueta = if (f.numero.isBlank()) "Número de la tarjeta" else "Número de la tarjeta · ${f.marca.etiqueta}",
            valor = f.numero,
            onCambio = vm::onNumero,
            error = f.errores.numero,
            placeholder = "4242 4242 4242 4242",
            tipoTeclado = KeyboardType.Number,
            enabled = !f.guardando,
            icono = "💳",
            transformacion = FormatoNumeroTarjeta,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                CampoTexto(
                    etiqueta = "Vencimiento",
                    valor = f.vencimiento,
                    onCambio = vm::onVencimiento,
                    error = f.errores.vencimiento,
                    placeholder = "MM/AA",
                    tipoTeclado = KeyboardType.Number,
                    enabled = !f.guardando,
                    transformacion = FormatoVencimiento,
                )
            }
            Column(Modifier.weight(1f)) {
                CampoTexto(
                    etiqueta = "CVV",
                    valor = f.cvv,
                    onCambio = vm::onCvv,
                    error = f.errores.cvv,
                    placeholder = if (f.marca == MarcaTarjeta.AMEX) "4 dígitos" else "3 dígitos",
                    tipoTeclado = KeyboardType.NumberPassword,
                    esContrasena = true,
                    enabled = !f.guardando,
                )
            }
        }
        CampoTexto(
            etiqueta = "Nombre del titular",
            valor = f.titular,
            onCambio = vm::onTitular,
            error = f.errores.titular,
            placeholder = "Como aparece en la tarjeta",
            enabled = !f.guardando,
            icono = "👤",
        )
        CampoTexto(
            etiqueta = "Alias (opcional)",
            valor = f.alias,
            onCambio = vm::onAlias,
            error = null,
            placeholder = "Ej.: Personal",
            accionIme = ImeAction.Done,
            enabled = !f.guardando,
            onListo = vm::agregar,
            icono = "🏷️",
        )
        Text(
            "Guardamos solo la marca, los últimos 4 dígitos y el vencimiento. El número completo y el CVV nunca se guardan.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
        Spacer(Modifier.height(14.dp))
        Boton(
            texto = if (f.guardando) "Guardando…" else "Guardar tarjeta",
            onClick = vm::agregar,
            estilo = "acento",
            enabled = !f.guardando,
        )
        Spacer(Modifier.height(8.dp))
        Boton("Cancelar", onClick = vm::cerrarFormulario, estilo = "fantasma", enabled = !f.guardando)
    }
}
