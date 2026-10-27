package cl.duoc.casajojo.navigation

/**
 * Lista cerrada de pantallas de la app (guía navegación).
 *
 * Cada pantalla guarda su "ruta", el texto que la identifica en el NavHost.
 * Al ser una sealed class, solo existen estas opciones: si alguien escribe una
 * pantalla que no existe, el compilador lo marca. Antes las rutas eran textos
 * sueltos ("home", "acom"…) y un error de tipeo solo se notaba al ejecutar.
 */
sealed class AppRoutes(val route: String) {
    // Sesión
    data object Login : AppRoutes("login")
    data object Registro : AppRoutes("registro")

    // Flujo del cliente
    data object Home : AppRoutes("home")
    data object Proteina : AppRoutes("proteina")
    data object Acompanamientos : AppRoutes("acompanamientos")
    data object Carrito : AppRoutes("carrito")
    data object Retiro : AppRoutes("retiro")
    data object Confirmado : AppRoutes("confirmado")
    data object MiPedido : AppRoutes("mi_pedido")
    data object Perfil : AppRoutes("perfil")
    data object Historial : AppRoutes("historial")
}
