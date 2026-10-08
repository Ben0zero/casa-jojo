package cl.duoc.casajojo.data.repo

import cl.duoc.casajojo.data.CasaJojoDatabase
import cl.duoc.casajojo.data.entity.Pedido
import cl.duoc.casajojo.data.entity.PedidoWithPlatos
import cl.duoc.casajojo.data.entity.Plato
import cl.duoc.casajojo.data.entity.Producto
import cl.duoc.casajojo.domain.PlatoDraft
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PedidoRepository(private val db: CasaJojoDatabase) {
    private val productos = db.productoDao()
    private val pedidos = db.pedidoDao()

    val proteinas: Flow<List<Producto>> =
        productos.todos().map { list -> list.filter { it.tipo == Producto.TIPO_PROTEINA } }

    val acompanamientos: Flow<List<Producto>> =
        productos.todos().map { list ->
            list.filter { it.tipo == Producto.TIPO_ACOMPANAMIENTO && it.disponible }
        }

    val ultimoPedido: Flow<PedidoWithPlatos?> = pedidos.ultimoConPlatos()

    suspend fun registrarPedido(
        nombre: String,
        horaRetiro: String,
        formaPago: String,
        indicaciones: String,
        platos: List<PlatoDraft>,
    ): Int {
        val numero = (pedidos.maxNumero() ?: 0) + 1
        val total = platos.sumOf { it.precio }
        val pedido = Pedido(
            numero = numero,
            nombre = nombre,
            horaRetiro = horaRetiro,
            formaPago = formaPago,
            indicaciones = indicaciones,
            estadoPedido = "recibido",
            estadoPago = "pendiente",
            total = total,
            fecha = System.currentTimeMillis(),
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
        return numero
    }
}