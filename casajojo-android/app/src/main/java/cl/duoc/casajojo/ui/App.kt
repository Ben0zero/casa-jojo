package cl.duoc.casajojo.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import cl.duoc.casajojo.ui.components.BarraNavegacion
import cl.duoc.casajojo.ui.components.PestanaNav
import cl.duoc.casajojo.ui.screens.cliente.AccionesPerfil
import cl.duoc.casajojo.ui.screens.cliente.ViewAccesibilidad
import cl.duoc.casajojo.ui.screens.cliente.ViewBilletera
import cl.duoc.casajojo.ui.screens.cliente.ViewConfiguracionCuenta
import cl.duoc.casajojo.ui.screens.cliente.ViewPrivacidad
import cl.duoc.casajojo.ui.screens.cliente.ViewQuienesSomos
import cl.duoc.casajojo.ui.theme.LocalReducirAnimaciones
import cl.duoc.casajojo.ui.viewmodels.AccesibilidadViewModel
import cl.duoc.casajojo.ui.viewmodels.BilleteraViewModel
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.core.content.FileProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cl.duoc.casajojo.ui.viewmodels.AuthViewModel
import cl.duoc.casajojo.ui.viewmodels.EstadoSesion
import cl.duoc.casajojo.ui.screens.auth.LoginScreen
import cl.duoc.casajojo.ui.screens.auth.RecuperarScreen
import cl.duoc.casajojo.ui.screens.auth.RegistroScreen
import cl.duoc.casajojo.ui.viewmodels.PerfilViewModel
import cl.duoc.casajojo.ui.viewmodels.ClienteViewModel
import cl.duoc.casajojo.ui.screens.cliente.ViewAcompanamientos
import cl.duoc.casajojo.ui.screens.cliente.ViewCarrito
import cl.duoc.casajojo.ui.screens.cliente.ViewConfirmado
import cl.duoc.casajojo.ui.screens.cliente.ViewHistorial
import cl.duoc.casajojo.ui.screens.cliente.ViewHome
import cl.duoc.casajojo.ui.screens.cliente.ViewMiPedido
import cl.duoc.casajojo.ui.screens.cliente.ViewProteina
import cl.duoc.casajojo.ui.screens.cliente.ViewRetiro
import cl.duoc.casajojo.ui.screens.cliente.ViewPerfil
import cl.duoc.casajojo.navigation.AppRoutes
import cl.duoc.casajojo.ui.components.PedirPermisoRedLocal
import cl.duoc.casajojo.ui.theme.CasaJojoTheme
import cl.duoc.casajojo.data.local.PerfilPhotoStorage

/**
 * Punto de entrada de la UI y mapa de navegación de la app.
 *
 *            ┌──────── sin sesión ────────┐
 *   abrir ──►│ login ◄──► registro        │
 *            │   └──► recuperar_password  │──(ingresa / se registra)──┐
 *            └────────────────────────────┘                           ▼
 *   abrir con sesión guardada ──────────────────────────────────────► home
 *
 *   home ──► proteina ──► acompanamientos ──► carrito ──► retiro ──► confirmado ──► mi_pedido
 *              ▲  └──────(solo proteína)──────────┘  │
 *              └──────────── "Agregar otro" ─────────┘
 *
 *   home ── pestaña "Perfil" / avatar ──► perfil ──► configuracion_cuenta · billetera ·
 *                                                   privacidad · accesibilidad · quienes_somos ·
 *                                                   historial · mi_pedido
 *   perfil ── "Cerrar sesión" ──► login (se borra la pila de pantallas)
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
    val context = LocalContext.current
    // Foto de perfil: única fuente de verdad (ruta en DataStore expuesta como StateFlow).
    // Home y Perfil leen el mismo valor, así que se actualiza sin reiniciar la app.
    val fotos = remember { PerfilPhotoStorage.get(context) }
    val fotoPerfil by fotos.foto.collectAsState()

    // Accesibilidad: un solo ViewModel para toda la app. CasaJojoTheme aplica sus
    // opciones (tema, texto grande, animaciones) y la pantalla Accesibilidad las cambia.
    val accesibilidadVm: AccesibilidadViewModel = viewModel()
    val opciones by accesibilidadVm.opciones.collectAsState()
    // Billetera: compartida entre la pantalla Billetera y el paso Retiro y pago.
    val billeteraVm: BilleteraViewModel = viewModel()

    val sesion by authVm.estadoSesion.collectAsState()

    // Android 17+: sin permiso de red local la app no alcanza los microservicios de
    // la PC. Al concederlo se vuelve a sincronizar el menú y los pedidos.
    PedirPermisoRedLocal(onConcedido = { clienteVm.sincronizarConServidor() })

    CasaJojoTheme(opciones = opciones) {
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
                    EstadoSesion.Cargando -> CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    else -> {
                        // remember: la pantalla inicial se decide UNA vez. Después, entrar o
                        // salir de la sesión se hace navegando, no recreando el NavHost.
                        val inicio = remember {
                            if (sesion is EstadoSesion.Activa) AppRoutes.Home.route else AppRoutes.Login.route
                        }
                        NavegacionApp(
                            rememberNavController(), inicio, authVm, clienteVm, fotos, fotoPerfil,
                            accesibilidadVm, billeteraVm,
                        )
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
    fotos: PerfilPhotoStorage,
    fotoPerfil: PerfilPhotoStorage.Foto?,
    accesibilidadVm: AccesibilidadViewModel,
    billeteraVm: BilleteraViewModel,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val reducir = LocalReducirAnimaciones.current
    val tarjetas by billeteraVm.tarjetas.collectAsState()
    val usuario by vm.usuario.collectAsState()

    /** Cerrar sesión: borra la sesión de DataStore y deja SOLO el login en la pila. */
    fun cerrarSesion() = authVm.cerrarSesion {
        vm.limpiarTodo() // descarta el carrito del usuario anterior
        nav.navigate(AppRoutes.Login.route) {
            popUpTo(nav.graph.id) { inclusive = true }
        }
    }

    /** Ir a una pestaña de la barra inferior sin apilar copias de la misma pantalla. */
    fun irAPestana(ruta: String) {
        if (!nav.popBackStack(ruta, false)) {
            nav.navigate(ruta) { launchSingleTop = true }
        }
    }

    /** Barra inferior con "Perfil" marcado: se usa en el Perfil y en todas sus subpantallas. */
    val barraPerfil: @Composable () -> Unit = {
        BarraNavegacion(
            onInicio = { nav.popBackStack(AppRoutes.Home.route, false) },
            onMenu = { nav.navigate(AppRoutes.Proteina.route) },
            onPedido = { irAPestana(AppRoutes.MiPedido.route) },
            onPerfil = { irAPestana(AppRoutes.Perfil.route) },
            activa = PestanaNav.PERFIL,
        )
    }

    /**
     * "Volver" de Mi pedido / Historial: si se llegó desde el Perfil, vuelve al Perfil;
     * si no, a la portada (comportamiento de siempre).
     */
    fun volverDesdePedidos() {
        if (nav.previousBackStackEntry?.destination?.route == AppRoutes.Perfil.route) {
            nav.popBackStack()
        } else {
            nav.popBackStack(AppRoutes.Home.route, false)
        }
    }

    /** Ir al inicio borrando todo lo anterior (el botón atrás ya no vuelve al login). */
    fun irAlInicioLimpiando() = nav.navigate(AppRoutes.Home.route) {
        popUpTo(nav.graph.id) { inclusive = true }
        launchSingleTop = true
    }

    NavHost(
        navController = nav,
        startDestination = inicio,
        // ANIMACIONES: transición entre pantallas del flujo.
        // Avanzar → la pantalla nueva entra deslizándose desde la derecha.
        // Volver  → sale deslizándose hacia la derecha (sensación de "atrás").
        // Accesibilidad: con "Reducir animaciones" las pantallas cambian sin transición.
        enterTransition = {
            if (reducir) EnterTransition.None
            else slideInHorizontally(
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 320f),
            ) { it / 6 } + fadeIn(tween(240))
        },
        exitTransition = { if (reducir) ExitTransition.None else fadeOut(tween(150)) },
        popEnterTransition = { if (reducir) EnterTransition.None else fadeIn(tween(200)) },
        popExitTransition = {
            if (reducir) ExitTransition.None
            else slideOutHorizontally(
                animationSpec = spring(dampingRatio = 0.85f, stiffness = 340f),
            ) { it / 6 } + fadeOut(tween(200))
        },
    ) {

        // ---------- SESIÓN ----------

        composable(AppRoutes.Login.route) {
            LoginScreen(
                vm = authVm,
                onIngresar = { irAlInicioLimpiando() },
                onIrRegistro = { nav.navigate(AppRoutes.Registro.route) },
                onOlvide = {
                    authVm.abrirRecuperacion()
                    nav.navigate(AppRoutes.RecuperarPassword.route)
                },
            )
        }

        // EFT: recuperación de contraseña; al terminar vuelve al login con el correo listo.
        composable(AppRoutes.RecuperarPassword.route) {
            RecuperarScreen(
                vm = authVm,
                onVolver = { nav.popBackStack() },
                onRestablecida = { nav.popBackStack(AppRoutes.Login.route, false) },
            )
        }

        composable(AppRoutes.Registro.route) {
            RegistroScreen(
                vm = authVm,
                onVolver = { nav.popBackStack() },
                onRegistrado = { irAlInicioLimpiando() },
            )
        }

        // ---------- FLUJO DEL CLIENTE ----------

        // PASO 0 — Portada
        composable(AppRoutes.Home.route) {
            ViewHome(
                vm = vm,
                onComenzar = { nav.navigate(AppRoutes.Proteina.route) },
                onMiPedido = { nav.navigate(AppRoutes.MiPedido.route) },
                onPerfil = { nav.navigate(AppRoutes.Perfil.route) { launchSingleTop = true } },
                fotoPerfil = fotoPerfil,
            )
        }

        // ---------- PERFIL (estilo Uber) Y SUS SUBPANTALLAS ----------

        composable(AppRoutes.Perfil.route) {
            ViewPerfil(
                usuario = usuario,
                foto = fotoPerfil,
                uriCaptura = {
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fotos",
                        fotos.archivoCaptura(),
                    )
                },
                onFotoTomada = { onResultado ->
                    scope.launch { onResultado(fotos.guardarCaptura()) }
                },
                onCerrarSesion = { cerrarSesion() },
                acciones = AccionesPerfil(
                    onConfiguracion = { nav.navigate(AppRoutes.ConfiguracionCuenta.route) },
                    onHistorial = { nav.navigate(AppRoutes.Historial.route) },
                    onMiPedido = { nav.navigate(AppRoutes.MiPedido.route) },
                    onBilletera = { nav.navigate(AppRoutes.Billetera.route) },
                    onPrivacidad = { nav.navigate(AppRoutes.Privacidad.route) },
                    onAccesibilidad = { nav.navigate(AppRoutes.Accesibilidad.route) },
                    onQuienesSomos = { nav.navigate(AppRoutes.QuienesSomos.route) },
                ),
                barra = barraPerfil,
            )
        }

        // Datos, contraseña y eliminar cuenta (misma lógica que antes, contra ms-auth).
        composable(AppRoutes.ConfiguracionCuenta.route) {
            // EFT: un PerfilViewModel por visita a la pantalla (se refresca desde ms-auth).
            val perfilVm: PerfilViewModel = viewModel()
            // Se recuerda la foto de esta cuenta: al eliminarla, la sesión (y el StateFlow)
            // pasan a null antes del callback, pero la foto igual debe borrarse del teléfono.
            val fotoDeLaCuenta = remember { mutableStateOf<PerfilPhotoStorage.Foto?>(null) }
            if (fotoPerfil != null) fotoDeLaCuenta.value = fotoPerfil
            ViewConfiguracionCuenta(
                vm = perfilVm,
                onVolver = { nav.popBackStack() },
                onCuentaEliminada = {
                    fotoDeLaCuenta.value?.let { scope.launch { fotos.borrar(it) } }
                    vm.limpiarTodo()
                    nav.navigate(AppRoutes.Login.route) {
                        popUpTo(nav.graph.id) { inclusive = true }
                    }
                },
                barra = barraPerfil,
            )
        }

        composable(AppRoutes.Billetera.route) {
            ViewBilletera(vm = billeteraVm, onVolver = { nav.popBackStack() }, barra = barraPerfil)
        }

        composable(AppRoutes.Privacidad.route) {
            ViewPrivacidad(
                tieneFoto = fotoPerfil != null,
                onEliminarFoto = { fotoPerfil?.let { scope.launch { fotos.borrar(it) } } },
                onEliminarCuenta = { nav.navigate(AppRoutes.ConfiguracionCuenta.route) },
                onVolver = { nav.popBackStack() },
                barra = barraPerfil,
            )
        }

        composable(AppRoutes.Accesibilidad.route) {
            ViewAccesibilidad(vm = accesibilidadVm, onVolver = { nav.popBackStack() }, barra = barraPerfil)
        }

        composable(AppRoutes.QuienesSomos.route) {
            ViewQuienesSomos(onVolver = { nav.popBackStack() }, barra = barraPerfil)
        }

        // PASO 1 — Proteína. Plato normal → Acompañamientos; "solo proteína" → Carrito.
        composable(AppRoutes.Proteina.route) {
            ViewProteina(
                vm = vm,
                onVolver = { nav.popBackStack() },
                onIrAcom = { nav.navigate(AppRoutes.Acompanamientos.route) },
                onIrCarrito = { nav.navigate(AppRoutes.Carrito.route) },
            )
        }

        // PASO 2 — Acompañamientos. Solo avanza si el ViewModel aceptó el plato.
        composable(AppRoutes.Acompanamientos.route) {
            ViewAcompanamientos(
                vm = vm,
                onVolver = { nav.popBackStack() },
                onAgregar = {
                    if (vm.agregarPlato()) {
                        nav.navigate(AppRoutes.Carrito.route)
                    }
                },
            )
        }

        // PASO 3 — Carrito. "Volver" regresa a la portada.
        composable(AppRoutes.Carrito.route) {
            ViewCarrito(
                vm = vm,
                onVolver = { nav.popBackStack(AppRoutes.Home.route, false) },
                onAgregarOtro = { nav.navigate(AppRoutes.Proteina.route) },
                onContinuar = { nav.navigate(AppRoutes.Retiro.route) },
            )
        }

        // PASO 4 — Retiro y pago. Al confirmar se limpia la pila hasta home,
        // para que "atrás" no vuelva a un formulario ya enviado.
        composable(AppRoutes.Retiro.route) {
            ViewRetiro(
                vm = vm,
                tarjetas = tarjetas,
                onIrBilletera = { nav.navigate(AppRoutes.Billetera.route) },
                onVolver = { nav.popBackStack() },
                onConfirmar = {
                    vm.confirmarPedido {
                        nav.navigate(AppRoutes.Confirmado.route) {
                            popUpTo(AppRoutes.Home.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                },
            )
        }

        // PASO 5 — Confirmación
        composable(AppRoutes.Confirmado.route) {
            ViewConfirmado(
                vm = vm,
                onConsultar = { nav.navigate(AppRoutes.MiPedido.route) },
                onOtroPedido = { nav.popBackStack(AppRoutes.Home.route, false) },
            )
        }

        // PASO 6 — Estado del último pedido del usuario
        composable(AppRoutes.MiPedido.route) {
            ViewMiPedido(
                vm = vm,
                onVolver = { volverDesdePedidos() },
                onCart = { nav.navigate(AppRoutes.Carrito.route) },
                onEmpezar = { nav.popBackStack(AppRoutes.Home.route, false) },
                onHistorial = { nav.navigate(AppRoutes.Historial.route) },
            )
        }

        // PASO 7 — Historial (RF-14): entregados y cancelados del usuario.
        composable(AppRoutes.Historial.route) {
            ViewHistorial(
                vm = vm,
                onVolver = { volverDesdePedidos() },
                onCart = { nav.navigate(AppRoutes.Carrito.route) },
            )
        }
    }
}
