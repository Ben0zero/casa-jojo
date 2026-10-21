package cl.duoc.casajojo.data.repo

import androidx.room.withTransaction
import cl.duoc.casajojo.data.CasaJojoDatabase
import cl.duoc.casajojo.data.entity.Pedido
import cl.duoc.casajojo.data.entity.PedidoWithPlatos
import cl.duoc.casajojo.data.entity.Plato
import cl.duoc.casajojo.data.entity.Producto
import cl.duoc.casajojo.domain.PlatoDraft
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