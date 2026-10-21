package cl.duoc.casajojo.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cl.duoc.casajojo.ui.auth.AuthViewModel
import cl.duoc.casajojo.ui.auth.EstadoSesion
import cl.duoc.casajojo.ui.auth.LoginScreen
import cl.duoc.casajojo.ui.auth.RegistroScreen
import cl.duoc.casajojo.ui.cliente.ClienteViewModel
import cl.duoc.casajojo.ui.cliente.ViewAcompanamientos
import cl.duoc.casajojo.ui.cliente.ViewCarrito
import cl.duoc.casajojo.ui.cliente.ViewConfirmado
import cl.duoc.casajojo.ui.cliente.ViewHome
import cl.duoc.casajojo.ui.cliente.ViewMiPedido
import cl.duoc.casajojo.ui.cliente.ViewProteina
import cl.duoc.casajojo.ui.cliente.ViewRetiro
import cl.duoc.casajojo.ui.navegacion.Screen
import cl.duoc.casajojo.ui.theme.CasaJojoTheme

/**
 * Punto de entrada de la UI y mapa de navegación de la app.
 *
 *            ┌──────── sin sesión ────────┐
 *   abrir ──►│ login ◄──► registro        │──(ingresa / se registra)──┐
 *            └────────────────────────────┘                           ▼
 *   abrir con sesión guardada ──────────────────────────────────────► home
 *
 *   home ──► proteina ──► acompanamientos ──► carrito ──► retiro ──► confirmado ──► mi_pedido
 *              ▲  └──────(solo proteína)──────────┘  │
 *              └──────────── "Agregar otro" ─────────┘
 *
 *   home ── "Cerrar sesión" ──► login (se borra la pila de pantallas)
 *
 * Las pantallas NO navegan solas: reciben lambdas (onVolver, onContinuar…) y
 * aquí se decide a dónde lleva cada una.
 */
@Composable
fun CasaJojoApp() {
    // AuthViewModel: login, registro y estado de la sesión (DataStore).
    val authVm: AuthViewModel = viewModel()
    // ClienteViewModel: carrito y pedidos. Uno solo para todo el flujo.
    val clienteVm: ClienteViewModel = viewModel()

    val sesion by authVm.estadoSesion.collectAsState()

    CasaJojoTheme {
        // Surface pinta el fondo y define el color de texto por defecto
        // (claro u oscuro según el tema).
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            // safeDrawingPadding: deja libre la barra de estado, la barra de gestos
            // y el teclado, para que nada quede tapado (la app usa edge-to-edge).
            Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                when (sesion) {
                    // Mientras DataStore se lee (una fracción de segundo) mostramos un loader,
                    // así no aparece el login "parpadeando" a alguien que ya tenía sesión.
                    EstadoSesion.Cargando -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    else -> {
                        // remember: la pantalla inicial se decide UNA vez. Después, entrar o
                        // salir de la sesión se hace navegando, no recreando el NavHost.
                        val inicio = remember {
                            if (sesion is EstadoSesion.Activa) Screen.Home.route else Screen.Login.route
                        }
                        NavegacionApp(rememberNavController(), inicio, authVm, clienteVm)
                    }
                }
            }
        }
    }
}

@Composable
private fun NavegacionApp(
    nav: NavHostController,
    inicio: String,
    authVm: AuthViewModel,
    vm: ClienteViewModel,
) {
    /** Ir al inicio borrando todo lo anterior (el botón atrás ya no vuelve al login). */
    fun irAlInicioLimpiando() = nav.navigate(Screen.Home.route) {
        popUpTo(nav.graph.id) { inclusive = true }
        launchSingleTop = true
    }

    NavHost(nav, startDestination = inicio) {

        // ---------- SESIÓN ----------

        composable(Screen.Login.route) {
            LoginScreen(
                vm = authVm,
                onIngresar = { irAlInicioLimpiando() },
                onIrRegistro = { nav.navigate(Screen.Registro.route) },
            )
        }

        composable(Screen.Registro.route) {
            RegistroScreen(
                vm = authVm,
                onVolver = { nav.popBackStack() },
                onRegistrado = { irAlInicioLimpiando() },
            )
        }

        // ---------- FLUJO DEL CLIENTE ----------

        // PASO 0 — Portada
        composable(Screen.Home.route) {
            ViewHome(
                vm = vm,
                onComenzar = { nav.navigate(Screen.Proteina.route) },
                onMiPedido = { nav.navigate(Screen.MiPedido.route) },
                onCerrarSesion = {
                    authVm.cerrarSesion {
                        vm.limpiarTodo() // descarta el carrito del usuario anterior
                        nav.navigate(Screen.Login.route) {
                            popUpTo(nav.graph.id) { inclusive = true }
                        }
                    }
                },
            )
        }

        // PASO 1 — Proteína. Plato normal → Acompañamientos; "solo proteína" → Carrito.
        composable(Screen.Proteina.route) {
            ViewProteina(
                vm = vm,
                onVolver = { nav.popBackStack() },
                onIrAcom = { nav.navigate(Screen.Acompanamientos.route) },
                onIrCarrito = { nav.navigate(Screen.Carrito.route) },
            )
        }

        // PASO 2 — Acompañamientos. Solo avanza si el ViewModel aceptó el plato.
        composable(Screen.Acompanamientos.route) {
            ViewAcompanamientos(
                vm = vm,
                onVolver = { nav.popBackStack() },
                onAgregar = {
                    if (vm.agregarPlato()) {
                        nav.navigate(Screen.Carrito.route)
                    }
                },
            )
        }

        // PASO 3 — Carrito. "Volver" regresa a la portada.
        composable(Screen.Carrito.route) {
            ViewCarrito(
                vm = vm,
                onVolver = { nav.popBackStack(Screen.Home.route, false) },
                onAgregarOtro = { nav.navigate(Screen.Proteina.route) },
                onContinuar = { nav.navigate(Screen.Retiro.route) },
            )
        }

        // PASO 4 — Retiro y pago. Al confirmar se limpia la pila hasta home,
        // para que "atrás" no vuelva a un formulario ya enviado.
        composable(Screen.Retiro.route) {
            ViewRetiro(
                vm = vm,
                onVolver = { nav.popBackStack() },
                onConfirmar = {
                    vm.confirmarPedido {
                        nav.navigate(Screen.Confirmado.route) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                },
            )
        }

        // PASO 5 — Confirmación
        composable(Screen.Confirmado.route) {
            ViewConfirmado(
                vm = vm,
                onConsultar = { nav.navigate(Screen.MiPedido.route) },
                onOtroPedido = { nav.popBackStack(Screen.Home.route, false) },
            )
        }

        // PASO 6 — Estado del último pedido del usuario
        composable(Screen.MiPedido.route) {
            ViewMiPedido(
                vm = vm,
                onVolver = { nav.popBackStack(Screen.Home.route, false) },
                onCart = { nav.navigate(Screen.Carrito.route) },
                onEmpezar = { nav.popBackStack(Screen.Home.route, false) },
            )
        }
    }
}