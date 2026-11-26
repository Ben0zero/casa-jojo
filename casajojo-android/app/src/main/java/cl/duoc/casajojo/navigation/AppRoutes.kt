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
    // EFT: "¿Olvidaste tu contraseña?"
    data object RecuperarPassword : AppRoutes("recuperar_password")

    // Flujo del cliente
    data object Home : AppRoutes("home")
    data object Proteina : AppRoutes("proteina")
    data object Acompanamientos : AppRoutes("acompanamientos")
    data object Carrito : AppRoutes("carrito")
    data object Retiro : AppRoutes("retiro")
    data object Confirmado : AppRoutes("confirmado")
    data object MiPedido : AppRoutes("mi_pedido")
    data object Historial : AppRoutes("historial")
    data object Perfil : AppRoutes("perfil")

    // Subpantallas del Perfil (estilo Uber). La pestaña "Perfil" queda marcada en todas.
    data object ConfiguracionCuenta : AppRoutes("configuracion_cuenta")
    data object Billetera : AppRoutes("billetera")
    data object Privacidad : AppRoutes("privacidad")
    data object Accesibilidad : AppRoutes("accesibilidad")
    data object QuienesSomos : AppRoutes("quienes_somos")
}
