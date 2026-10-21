package cl.duoc.casajojo.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cl.duoc.casajojo.ui.cliente.ClienteViewModel
import cl.duoc.casajojo.ui.cliente.ViewAcompanamientos
import cl.duoc.casajojo.ui.cliente.ViewCarrito
import cl.duoc.casajojo.ui.cliente.ViewConfirmado
import cl.duoc.casajojo.ui.cliente.ViewHome
import cl.duoc.casajojo.ui.cliente.ViewMiPedido
import cl.duoc.casajojo.ui.cliente.ViewProteina
import cl.duoc.casajojo.ui.cliente.ViewRetiro
import cl.duoc.casajojo.ui.theme.CasaJojoTheme

/**
 * Punto de entrada de la UI y mapa de navegación de la app.
 *
 * Flujo del cliente:
 *
 *   home ──► proteina ──► acom ──► carrito ──► retiro ──► confirmado ──► mi_pedido
 *              ▲  │                  │  ▲                                  ▲
 *              │  └─(solo proteína)──┘  │                                  │
 *              └──── "Agregar otro" ────┘           home ── "Ver mi pedido"┘
 *
 * Cada pantalla NO navega por sí misma: recibe lambdas (onVolver, onContinuar…)
 * y aquí decidimos a dónde lleva cada una. Así las pantallas quedan desacopladas
 * de la navegación y son más fáciles de reutilizar y probar.
 */
@Composable
fun CasaJojoApp() {
    // NavController: guarda la pila de pantallas visitadas (back stack).
    val nav = rememberNavController()
    // Un solo ViewModel para todo el flujo: el carrito se comparte entre pantallas.
    val vm: ClienteViewModel = viewModel()

    CasaJojoTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            NavHost(nav, startDestination = "home") {

                // PASO 0 — Portada
                composable("home") {
                    ViewHome(
                        vm = vm,
                        onComenzar = { nav.navigate("proteina") },
                        onMiPedido = { nav.navigate("mi_pedido") },
                    )
                }

                // PASO 1 — Elegir proteína.
                // Plato normal → Acompañamientos. "Solo proteína" → directo al Carrito.
                composable("proteina") {
                    ViewProteina(
                        vm = vm,
                        onVolver = { nav.popBackStack() },
                        onIrAcom = { nav.navigate("acom") },
                        onIrCarrito = { nav.navigate("carrito") },
                    )
                }

                // PASO 2 — Elegir acompañamientos.
                // Solo navega al carrito si el ViewModel aceptó el plato (validación OK).
                composable("acom") {
                    ViewAcompanamientos(
                        vm = vm,
                        onVolver = { nav.popBackStack() },
                        onAgregar = {
                            if (vm.agregarPlato()) {
                                nav.navigate("carrito")
                            }
                        },
                    )
                }

                // PASO 3 — Carrito.
                // "Volver" regresa a la portada (popBackStack("home", false) saca
                // de la pila todo lo que está sobre "home" sin quitar "home").
                composable("carrito") {
                    ViewCarrito(
                        vm = vm,
                        onVolver = { nav.popBackStack("home", false) },
                        onAgregarOtro = { nav.navigate("proteina") },
                        onContinuar = { nav.navigate("retiro") },
                    )
                }

                // PASO 4 — Retiro y pago.
                // confirmarPedido guarda en Room y, al terminar, ejecuta la lambda
                // que navega a Confirmado. popUpTo("home") limpia la pila para que
                // el botón atrás no vuelva al formulario de un pedido ya enviado.
                composable("retiro") {
                    ViewRetiro(
                        vm = vm,
                        onVolver = { nav.popBackStack() },
                        onConfirmar = {
                            vm.confirmarPedido {
                                nav.navigate("confirmado") {
                                    popUpTo("home") { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        },
                    )
                }

                // PASO 5 — Confirmación con el número del pedido.
                composable("confirmado") {
                    ViewConfirmado(
                        vm = vm,
                        onConsultar = { nav.navigate("mi_pedido") },
                        onOtroPedido = { nav.popBackStack("home", false) },
                    )
                }

                // PASO 6 — Consultar estado del último pedido.
                composable("mi_pedido") {
                    ViewMiPedido(
                        vm = vm,
                        onVolver = { nav.popBackStack("home", false) },
                        onCart = { nav.navigate("carrito") },
                        onEmpezar = { nav.popBackStack("home", false) },
                    )
                }
            }
        }
    }
}