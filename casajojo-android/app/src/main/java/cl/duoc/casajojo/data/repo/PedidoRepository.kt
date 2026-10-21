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
 * Repositorio: único punto de acceso a los datos para el ViewModel.
 * El ViewModel no conoce los DAO ni SQL; solo pide "proteínas",
 * "último pedido" o "registrar pedido".
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

    /** Último pedido con sus platos. Emite de nuevo cada vez que cambian las tablas. */
    val ultimoPedido: Flow<PedidoWithPlatos?> = pedidos.ultimoConPlatos()

    /**
     * Guarda un pedido completo y devuelve su número correlativo.
     * Estado inicial: pedido "recibido" y pago "pendiente" (el pago es solo una etiqueta).
     */
    suspend fun registrarPedido(
        nombre: String,
        horaRetiro: String,
        formaPago: String,
        indicaciones: String,
        platos: List<PlatoDraft>,
    ): Int =
    // CORRECCIÓN: withTransaction agrupa todas las inserciones en una sola
    // operación "todo o nada". Antes, si fallaba la inserción de un plato,
    // quedaba guardado un pedido sin platos (o con platos a medias).
    // Además, leer el máximo y crear el número dentro de la transacción
        // evita que dos registros simultáneos obtengan el mismo número.
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
            )
            // insertPedido devuelve el id generado; lo usamos como llave foránea de cada plato.
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