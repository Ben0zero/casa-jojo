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

@Composable
fun CasaJojoApp() {
    val nav = rememberNavController()
    val vm: ClienteViewModel = viewModel()
    CasaJojoTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            NavHost(nav, startDestination = "home") {
                composable("home") {
                    ViewHome(
                        vm = vm,
                        onComenzar = { nav.navigate("proteina") },
                        onMiPedido = { nav.navigate("mi_pedido") },
                    )
                }
                composable("proteina") {
                    ViewProteina(
                        vm = vm,
                        onVolver = { nav.popBackStack() },
                        onIrAcom = { nav.navigate("acom") },
                        onIrCarrito = { nav.navigate("carrito") },
                    )
                }
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
                composable("carrito") {
                    ViewCarrito(
                        vm = vm,
                        onVolver = { nav.popBackStack("home", false) },
                        onAgregarOtro = { nav.navigate("proteina") },
                        onContinuar = { nav.navigate("retiro") },
                    )
                }
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
                composable("confirmado") {
                    ViewConfirmado(
                        vm = vm,
                        onConsultar = { nav.navigate("mi_pedido") },
                        onOtroPedido = { nav.popBackStack("home", false) },
                    )
                }
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