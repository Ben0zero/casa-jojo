package cl.duoc.casajojo.ui.navegacion

/**
 * Lista cerrada de pantallas de la app (guía navegación).
 *
 * Cada pantalla guarda su "ruta", el texto que la identifica en el NavHost.
 * Al ser una sealed class, solo existen estas opciones: si alguien escribe una
 * pantalla que no existe, el compilador lo marca. Antes las rutas eran textos
 * sueltos ("home", "acom"…) y un error de tipeo solo se notaba al ejecutar.
 */
sealed class Screen(val route: String) {
    // Sesión
    data object Login : Screen("login")
    data object Registro : Screen("registro")

    // Flujo del cliente
    data object Home : Screen("home")
    data object Proteina : Screen("proteina")
    data object Acompanamientos : Screen("acompanamientos")
    data object Carrito : Screen("carrito")
    data object Retiro : Screen("retiro")
    data object Confirmado : Screen("confirmado")
    data object MiPedido : Screen("mi_pedido")
}