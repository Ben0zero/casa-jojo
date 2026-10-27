package cl.duoc.casajojo.data.repository

import androidx.room.withTransaction
import cl.duoc.casajojo.data.local.CasaJojoDatabase
import cl.duoc.casajojo.data.local.entity.Pedido
import cl.duoc.casajojo.data.local.entity.PedidoWithPlatos
import cl.duoc.casajojo.data.local.entity.Plato
import cl.duoc.casajojo.data.local.entity.Producto
import cl.duoc.casajojo.domain.model.PlatoDraft
import cl.duoc.casajojo.domain.usecase.ReglasPedido
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repositorio: único punto de acceso a los datos de menú y pedidos para el ViewModel.
 * El ViewModel no conoce los DAO ni SQL.
 */
class PedidoRepository(private val db: CasaJojoDatabase) {
    private val productos = db.productoDao()
    private val pedidos = db.pedidoDao()

    /** Todas las proteínas, incluidas las agotadas (se muestran tachadas). */
    val proteinas: Flow<List<Producto>> =
        productos.todos().map { list -> list.filter { it.tipo == Producto.TIPO_PROTEINA } }

    /** Solo acompañamientos disponibles: los agotados no se ofrecen. */
    val acompanamientos: Flow<List<Producto>> =
        productos.todos().map { list ->
            list.filter { it.tipo == Producto.TIPO_ACOMPANAMIENTO && it.disponible }
        }

    /** CAMBIO (login): último pedido del usuario indicado. */
    fun ultimoPedidoDe(usuarioId: Long): Flow<PedidoWithPlatos?> =
        pedidos.ultimoDelUsuario(usuarioId)

    /** NUEVO: todos los pedidos del usuario, el más reciente primero. */
    fun pedidosDe(usuarioId: Long): Flow<List<PedidoWithPlatos>> =
        pedidos.todosDelUsuario(usuarioId)

    /** NUEVO (RF-14): pedidos del usuario en un estado, para el Historial. */
    fun historialDe(usuarioId: Long, estado: String): Flow<List<PedidoWithPlatos>> =
        pedidos.delUsuarioPorEstado(usuarioId, estado)

    /**
     * NUEVO (RF-13): cancela el pedido si aún está en plazo.
     * La regla se vuelve a revisar en el UPDATE, así que aunque la pantalla
     * se atrase un segundo, nunca se cancela fuera de los 15 s.
     * Devuelve true si se canceló.
     */
    suspend fun cancelarPedido(id: Long, ahora: Long = System.currentTimeMillis()): Boolean =
        pedidos.cancelar(id, ReglasPedido.limiteCancelacion(ahora)) > 0

    /** NUEVO (RF-14): retiro con QR → "entregado" y "pagado". true si cambió. */
    suspend fun marcarEntregado(id: Long): Boolean =
        pedidos.marcarEntregado(id) > 0

    /**
     * Guarda un pedido completo y devuelve su número correlativo.
     * Estado inicial: pedido "recibido" y pago "pendiente".
     * CAMBIO (login): recibe usuarioId para asociar el pedido a la cuenta.
     */
    suspend fun registrarPedido(
        usuarioId: Long?,
        nombre: String,
        horaRetiro: String,
        formaPago: String,
        indicaciones: String,
        platos: List<PlatoDraft>,
    ): Int =
        // withTransaction: todo o nada (pedido + platos), y el número no se repite.
        db.withTransaction {
            val numero = (pedidos.maxNumero() ?: 0) + 1
            val pedido = Pedido(
                numero = numero,
                nombre = nombre,
                horaRetiro = horaRetiro,
                formaPago = formaPago,
                indicaciones = indicaciones,
                estadoPedido = "recibido",
                estadoPago = "pendiente",
                total = platos.sumOf { it.precio },
                fecha = System.currentTimeMillis(),
                usuarioId = usuarioId,
            )
            val pedidoId = pedidos.insertPedido(pedido)
            platos.forEach { draft ->
                pedidos.insertPlato(
                    Plato(
                        pedidoId = pedidoId,
                        nombreProteina = draft.nombreProteina,
                        precio = draft.precio,
                        solo = draft.solo,
                        acompanamientos = draft.acompanamientos,
                    )
                )
            }
            numero
        }
}