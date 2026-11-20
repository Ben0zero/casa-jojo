package cl.duoc.casajojo.data.repository

import cl.duoc.casajojo.data.local.TransactionRunner
import cl.duoc.casajojo.data.local.dao.PedidoDao
import cl.duoc.casajojo.data.local.dao.ProductoDao
import cl.duoc.casajojo.data.local.entity.Pedido
import cl.duoc.casajojo.data.local.entity.PedidoWithPlatos
import cl.duoc.casajojo.data.local.entity.Plato
import cl.duoc.casajojo.data.local.entity.Producto
import cl.duoc.casajojo.data.remote.MenuApi
import cl.duoc.casajojo.data.remote.PedidoApi
import cl.duoc.casajojo.data.remote.RetrofitInstance
import cl.duoc.casajojo.data.remote.dto.PedidoItemRequestDto
import cl.duoc.casajojo.data.remote.dto.PedidoRequestDto
import cl.duoc.casajojo.data.remote.dto.PedidoResponseDto
import cl.duoc.casajojo.domain.model.PlatoDraft
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Repositorio de menú y pedidos (EA3 / IL3.1).
 *
 * La API es la fuente de verdad; Room conserva una copia para que la app
 * siga funcionando cuando el microservicio no responde.
 */
class PedidoRepository(
    private val productos: ProductoDao,
    private val pedidos: PedidoDao,
    private val transacciones: TransactionRunner,
    private val menuApi: MenuApi = RetrofitInstance.menuApi,
    private val pedidoApi: PedidoApi = RetrofitInstance.pedidoApi,
) {

    val proteinas: Flow<List<Producto>> =
        productos.todos().map { list -> list.filter { it.tipo == Producto.TIPO_PROTEINA } }

    val acompanamientos: Flow<List<Producto>> =
        productos.todos().map { list ->
            list.filter { it.tipo == Producto.TIPO_ACOMPANAMIENTO && it.disponible }
        }

    fun ultimoPedidoDe(usuarioId: Long): Flow<PedidoWithPlatos?> =
        pedidos.ultimoDelUsuario(usuarioId)

    /**
     * Descarga el menú de ms-catalogoProductos y reemplaza la caché de Room.
     * Devuelve false si la API no respondió (en ese caso se conserva la caché).
     */
    suspend fun sincronizarMenu(): Boolean {
        val remotos = try {
            menuApi.menu()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return false
        }
        val locales = remotos.mapNotNull { dto ->
            val id = dto.id ?: return@mapNotNull null
            val nombre = dto.nombre?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            Producto(
                id = id,
                nombre = nombre,
                precio = dto.precio ?: 0,
                tipo = if (dto.tipo.equals("acompanamiento", ignoreCase = true)) {
                    Producto.TIPO_ACOMPANAMIENTO
                } else {
                    Producto.TIPO_PROTEINA
                },
                disponible = dto.disponible ?: true,
            )
        }
        if (locales.isEmpty()) return false

        transacciones.ejecutar {
            productos.borrarTodo()
            productos.insertAll(locales)
        }
        return true
    }

    /**
     * Guarda el pedido en Room y lo envía a ms-ventas. Si la API falla, el
     * pedido queda igualmente registrado en el dispositivo (modo caché).
     */
    suspend fun registrarPedido(
        usuarioId: Long?,
        nombre: String,
        horaRetiro: String,
        formaPago: String,
        indicaciones: String,
        platos: List<PlatoDraft>,
        correo: String = "",
        origen: String = "Mostrador",
    ): Int {
        val numero = transacciones.ejecutar {
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

        try {
            pedidoApi.crear(
                PedidoRequestDto(
                    numero = numero,
                    nombre = nombre,
                    correo = correo,
                    horaRetiro = horaRetiro,
                    origen = origen,
                    formaPago = formaPago,
                    indicaciones = indicaciones,
                    items = platos.map {
                        PedidoItemRequestDto(
                            nombre = it.nombreProteina,
                            precio = it.precio,
                            solo = it.solo,
                            acompanamientos = it.acompanamientos,
                        )
                    },
                    total = platos.sumOf { it.precio },
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Sin red: el pedido ya está en Room y se reintenta en el próximo envío.
        }
        return numero
    }

    /** Trae el estado vigente desde ms-ventas y lo refleja en la caché local. */
    suspend fun sincronizarEstado(correo: String) {
        if (correo.isBlank()) return
        val remoto = try {
            pedidoApi.ultimo(correo)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return
        }
        val numero = remoto.numero ?: return
        val local = pedidos.porNumero(numero) ?: return
        pedidos.actualizar(local.copyEstado(remoto))
    }

    /**
     * CRUD real desde la app (PUT): elige "Listo para retiro" en Mi pedido y
     * el cambio viaja a ms-ventas. Buscamos el id remoto por número + correo
     * para no agregar columnas a Room (no necesitamos migrar la base de datos).
     * Si falla la red, Room conserva el estado anterior y la pantalla avisa.
     */
    suspend fun cambiarEstadoRemoto(
        correo: String,
        numero: Int,
        estado: String,
    ): Result<PedidoResponseDto> = withContext(Dispatchers.IO) {
        try {
            if (correo.isBlank()) error("Inicia sesión para actualizar el pedido")
            val remoto = pedidoApi.misPedidos(correo).firstOrNull { it.numero == numero }
                ?: error("No encontramos el pedido #$numero en el servidor")
            val id = remoto.id ?: error("El pedido #$numero no tiene id remoto")
            val respuesta = pedidoApi.cambiarEstado(id, estado)
            pedidos.porNumero(numero)?.let { local ->
                pedidos.actualizar(local.copyEstado(respuesta))
            }
            Result.success(respuesta)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun Pedido.copyEstado(remoto: PedidoResponseDto): Pedido = copy(
        estadoPedido = estadoLocal(remoto.estadoPedido),
        estadoPago = estadoPagoLocal(remoto.estadoPago),
    )

    private fun estadoPagoLocal(estado: String?): String = when (estado?.lowercase()) {
        "pagado" -> "pagado"
        "no_pagado" -> "no_pagado"
        else -> "pendiente"
    }

    private fun estadoLocal(estado: String?): String = when (estado?.uppercase()) {
        "RECIBIDO" -> "recibido"
        "EN_PREPARACION" -> "preparacion"
        "LISTO" -> "listo"
        "ENTREGADO" -> "entregado"
        "CANCELADO" -> "cancelado"
        else -> "recibido"
    }
}
