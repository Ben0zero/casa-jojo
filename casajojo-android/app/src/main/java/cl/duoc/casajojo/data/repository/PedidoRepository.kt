package cl.duoc.casajojo.data.repository

import cl.duoc.casajojo.domain.model.EstadoPedido
import cl.duoc.casajojo.data.local.TransactionRunner
import cl.duoc.casajojo.data.local.dao.PedidoDao
import cl.duoc.casajojo.data.local.dao.ProductoDao
import cl.duoc.casajojo.data.local.entity.Pedido
import cl.duoc.casajojo.data.local.entity.PedidoWithPlatos
import cl.duoc.casajojo.data.local.entity.Plato
import cl.duoc.casajojo.data.local.entity.Producto
import cl.duoc.casajojo.data.remote.ErroresRemotos
import cl.duoc.casajojo.data.remote.MenuApi
import cl.duoc.casajojo.data.remote.PedidoApi
import cl.duoc.casajojo.data.remote.RetrofitInstance
import cl.duoc.casajojo.data.remote.dto.PedidoEdicionDto
import cl.duoc.casajojo.data.remote.dto.PedidoItemRequestDto
import cl.duoc.casajojo.data.remote.dto.PedidoRequestDto
import cl.duoc.casajojo.data.remote.dto.PedidoResponseDto
import cl.duoc.casajojo.domain.error.CasaJojoException
import cl.duoc.casajojo.domain.error.ConflictoException
import cl.duoc.casajojo.domain.error.NoEncontradoException
import cl.duoc.casajojo.domain.error.SesionExpiradaException
import cl.duoc.casajojo.domain.error.ValidacionException
import cl.duoc.casajojo.domain.model.PlatoDraft
import cl.duoc.casajojo.domain.usecase.ReglasPedido
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.util.UUID

/**
 * Repositorio de menú y pedidos (EA3 / IL3.1).
 *
 * La API es la fuente de verdad; Room conserva una copia para que la app
 * siga funcionando cuando el microservicio no responde.
 *
 * Cómo se vincula un pedido de Room con el de Oracle (A4 / M9):
 * - Cada pedido nace con una "referencia" (UUID) que viaja a ms-ventas. El servidor
 *   la usa para no duplicar un pedido reenviado y la devuelve en cada respuesta.
 * - Cuando ms-ventas confirma, se guarda su id en "remotoId".
 * - Un pedido sin remotoId quedó pendiente de envío (sin red) y se reenvía solo.
 * - Ya NO se busca por número: el número puede repetirse entre teléfonos.
 */
class PedidoRepository(
    private val productos: ProductoDao,
    private val pedidos: PedidoDao,
    private val transacciones: TransactionRunner,
    private val menuApi: MenuApi = RetrofitInstance.menuApi,
    private val pedidoApi: PedidoApi = RetrofitInstance.pedidoApi,
    private val reloj: () -> Long = System::currentTimeMillis,
    private val nuevaReferencia: () -> String = { UUID.randomUUID().toString() },
) {

    val proteinas: Flow<List<Producto>> =
        productos.todos().map { list -> list.filter { it.tipo == Producto.TIPO_PROTEINA } }

    val acompanamientos: Flow<List<Producto>> =
        productos.todos().map { list ->
            list.filter { it.tipo == Producto.TIPO_ACOMPANAMIENTO && it.disponible }
        }

    fun ultimoPedidoDe(usuarioId: Long): Flow<PedidoWithPlatos?> =
        pedidos.ultimoDelUsuario(usuarioId)

    /** RF-14: todos los pedidos del usuario (RF-19: antes solo se veía el último). */
    fun pedidosDe(usuarioId: Long): Flow<List<PedidoWithPlatos>> =
        pedidos.todosDelUsuario(usuarioId)

    /** RF-14: pedidos en un estado concreto, para el Historial. */
    fun historialDe(usuarioId: Long, estado: String): Flow<List<PedidoWithPlatos>> =
        pedidos.delUsuarioPorEstado(usuarioId, estado)

    /**
     * RF-13: cancela solo si sigue en plazo. La regla se revisa otra vez en el
     * UPDATE de Room, así que un retraso de la UI no permite cancelar fuera de tiempo.
     */
    suspend fun cancelarPedido(id: Long, ahora: Long = reloj()): Boolean =
        pedidos.cancelar(id, ReglasPedido.limiteCancelacion(ahora)) > 0

    /** RF-14: retiro con QR → entregado y pagado. */
    suspend fun marcarEntregado(id: Long): Boolean =
        pedidos.marcarEntregado(id) > 0

    /**
     * Descarga el menú de ms-catalogo y reemplaza la caché de Room.
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
     * pedido queda registrado en el dispositivo como PENDIENTE (sin remotoId)
     * y se reenvía solo más tarde (ver [enviarPendientes]).
     *
     * M13: la "fecha" local (desde la que corren los 15 s para cancelar) se fija
     * cuando termina el intento de envío, no antes: así una red lenta no consume
     * el plazo mientras el cliente espera.
     */
    suspend fun registrarPedido(
        usuarioId: Long?,
        nombre: String,
        horaRetiro: String,
        formaPago: String,
        indicaciones: String,
        platos: List<PlatoDraft>,
        correo: String = "",
        origen: String = ORIGEN_APP,
    ): Int {
        val referencia = nuevaReferencia()
        val (pedidoId, numero) = transacciones.ejecutar {
            val numero = (pedidos.maxNumero() ?: 0) + 1
            val pedido = Pedido(
                numero = numero,
                nombre = nombre,
                horaRetiro = horaRetiro,
                formaPago = formaPago,
                indicaciones = indicaciones,
                estadoPedido = EstadoPedido.Recibido.codigo,
                estadoPago = "pendiente",
                total = platos.sumOf { it.precio },
                fecha = reloj(),
                usuarioId = usuarioId,
                referencia = referencia,
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
            pedidoId to numero
        }

        val items = platos.map {
            PedidoItemRequestDto(
                nombre = it.nombreProteina,
                precio = it.precio,
                solo = it.solo,
                acompanamientos = it.acompanamientos,
            )
        }
        val respuesta = intentarEnviar(
            PedidoRequestDto(
                numero = numero,
                nombre = nombre,
                correo = correo,
                horaRetiro = horaRetiro,
                origen = origen,
                formaPago = formaPago,
                indicaciones = indicaciones,
                items = items,
                total = platos.sumOf { it.precio },
                referencia = referencia,
            )
        )
        val remotoId = respuesta?.id
        if (remotoId != null) {
            pedidos.marcarEnviado(pedidoId, remotoId, reloj())
        } else {
            // Sin red: queda pendiente; el plazo de cancelación corre desde ahora.
            pedidos.actualizarFecha(pedidoId, reloj())
        }
        return numero
    }

    /**
     * A4: reenvía a ms-ventas los pedidos del usuario que quedaron solo en Room.
     * Gracias a la referencia, si el servidor ya lo tenía no se duplica.
     * Se detiene al primer error de red; un rechazo 4xx salta ese pedido.
     * Devuelve cuántos pedidos quedaron enviados.
     */
    suspend fun enviarPendientes(usuarioId: Long?, correo: String): Int {
        if (usuarioId == null || correo.isBlank()) return 0
        var enviados = 0
        for (pendiente in pedidos.pendientesDeEnvio(usuarioId)) {
            val p = pendiente.pedido
            val referencia = p.referencia.ifBlank { nuevaReferencia() }
            val request = PedidoRequestDto(
                numero = p.numero,
                nombre = p.nombre,
                correo = correo,
                horaRetiro = p.horaRetiro,
                origen = ORIGEN_APP,
                formaPago = p.formaPago,
                indicaciones = p.indicaciones,
                items = pendiente.platos.map {
                    PedidoItemRequestDto(
                        nombre = it.nombreProteina,
                        precio = it.precio,
                        solo = it.solo,
                        acompanamientos = it.acompanamientos,
                    )
                },
                total = p.total,
                referencia = referencia,
            )
            val respuesta = try {
                pedidoApi.crear(request)
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                if (e.code() in 400..499) continue else break
            } catch (e: Exception) {
                break // sin red: se intentará en la próxima sincronización
            }
            val remotoId = respuesta.id ?: continue
            // Se conserva la fecha original: reenviar no reabre el plazo de cancelación.
            pedidos.actualizar(p.copy(remotoId = remotoId, referencia = referencia).copyEstado(respuesta))
            enviados++
        }
        return enviados
    }

    /**
     * M11: trae TODOS los pedidos del cliente desde ms-ventas (no solo el último)
     * y refleja su estado en la caché local. Primero reenvía los pendientes (A4).
     * Solo toca pedidos del usuario indicado.
     */
    suspend fun sincronizarEstados(usuarioId: Long?, correo: String) {
        if (usuarioId == null || correo.isBlank()) return
        enviarPendientes(usuarioId, correo)
        val remotos = try {
            pedidoApi.misPedidos(correo)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return
        }
        for (local in pedidos.delUsuario(usuarioId)) {
            val remoto = emparejar(local, remotos) ?: continue
            val actualizado = local.copyEstado(remoto)
            if (actualizado != local) pedidos.actualizar(actualizado)
        }
    }

    /**
     * RF-13: cancelación dentro de 15 s.
     * - Si el pedido está en ms-ventas, se envía CANCELADO (el servidor revisa el plazo).
     * - M13 / A4: si el pedido nunca llegó al servidor (sin red al crearlo y no se
     *   encuentra por su referencia), se cancela solo en el teléfono y ya no se reenvía.
     */
    suspend fun cancelarPedidoRemoto(correo: String, pedido: Pedido): Result<PedidoResponseDto> =
        cancelarPedidoRemoto(correo, pedido, reloj())

    /** Igual que la anterior, pero con la hora indicada (la usan las pruebas del plazo). */
    suspend fun cancelarPedidoRemoto(
        correo: String,
        pedido: Pedido,
        ahora: Long,
    ): Result<PedidoResponseDto> = withContext(Dispatchers.IO) {
        try {
            if (!ReglasPedido.puedeCancelar(pedido.fecha, ahora, pedido.estadoPedido)) {
                throw ConflictoException(MSG_PLAZO_CANCELACION)
            }
            val id = pedido.remotoId ?: buscarEnServidor(correo, pedido, toleraSinRed = true)
            if (id == null) {
                // Solo existe en el teléfono: se cancela localmente.
                if (pedidos.cancelar(pedido.id, ReglasPedido.limiteCancelacion(reloj())) == 0) {
                    throw ConflictoException(MSG_PLAZO_CANCELACION)
                }
                return@withContext Result.success(
                    PedidoResponseDto(numero = pedido.numero, estadoPedido = "CANCELADO")
                )
            }
            val respuesta = pedidoApi.cambiarEstado(id, "CANCELADO")
            val filas = pedidos.cancelar(pedido.id, ReglasPedido.limiteCancelacion(reloj()))
            if (filas == 0) {
                // El remoto ya se canceló: alineamos Room aunque cambiara de estado.
                actualizarCache(pedido.id, respuesta)
            }
            Result.success(respuesta)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(traducirError(e, enConflicto = MSG_PLAZO_CANCELACION))
        }
    }

    /**
     * RF-14 / A5: retiro con el QR de la tienda. Se envía el código leído del QR a
     * PUT /api/pedidos/{id}/retirar; ms-ventas lo valida y deja el pedido pagado y
     * ENTREGADO en un solo paso. Si la red falla o el servidor rechaza, Room no se
     * toca y la pantalla avisa.
     */
    suspend fun marcarEntregadoRemoto(
        correo: String,
        pedido: Pedido,
        codigoQr: String,
    ): Result<PedidoResponseDto> = withContext(Dispatchers.IO) {
        try {
            if (!ReglasPedido.puedeRetirarConQr(pedido.estadoPedido)) {
                throw ConflictoException(MSG_NO_LISTO)
            }
            val id = pedido.remotoId ?: buscarEnServidor(correo, pedido, toleraSinRed = false)
                ?: throw NoEncontradoException("No encontramos el pedido #${pedido.numero} en el servidor")
            val respuesta = pedidoApi.retirar(id, codigoQr)
            if (pedidos.marcarEntregado(pedido.id) == 0) {
                actualizarCache(pedido.id, respuesta)
            }
            Result.success(respuesta)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(traducirError(e, enConflicto = MSG_NO_LISTO))
        }
    }

    /**
     * EFT / RF-12: editar nombre, hora de retiro, forma de pago e indicaciones mientras
     * el pedido siga "recibido". Si está en ms-ventas se edita allá (el servidor vuelve a
     * revisar el estado: 409 si la cocina ya empezó); si nunca llegó al servidor, se edita
     * solo en Room y se enviará con los datos nuevos. Room se actualiza al final.
     */
    suspend fun editarPedido(
        correo: String,
        pedido: Pedido,
        nombre: String,
        horaRetiro: String,
        formaPago: String,
        indicaciones: String,
    ): Result<Pedido> = withContext(Dispatchers.IO) {
        try {
            val error = ReglasPedido.validarEdicion(pedido.estadoPedido, nombre)
            if (error != null) {
                throw if (ReglasPedido.puedeEditar(pedido.estadoPedido)) {
                    ValidacionException(error, campo = "nombre")
                } else {
                    ConflictoException(error)
                }
            }
            val datos = PedidoEdicionDto(nombre.trim(), horaRetiro, formaPago, indicaciones.trim())
            val id = pedido.remotoId ?: buscarEnServidor(correo, pedido, toleraSinRed = true)
            val editado = if (id == null) {
                pedido.copy(
                    nombre = datos.nombre, horaRetiro = datos.horaRetiro,
                    formaPago = datos.formaPago, indicaciones = datos.indicaciones,
                )
            } else {
                val r = pedidoApi.editar(id, datos)
                pedido.copy(
                    remotoId = id,
                    nombre = r.nombre ?: datos.nombre,
                    horaRetiro = r.horaRetiro ?: datos.horaRetiro,
                    formaPago = r.formaPago ?: datos.formaPago,
                    indicaciones = r.indicaciones ?: datos.indicaciones,
                ).copyEstado(r)
            }
            pedidos.actualizar(editado)
            Result.success(editado)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (e is HttpException && e.code() == 409) {
                // La cocina ya lo tomó: se refresca el estado local para que la UI lo muestre.
                runCatching { sincronizarEstados(pedido.usuarioId, correo) }
            }
            Result.failure(traducirError(e, enConflicto = MSG_NO_EDITABLE))
        }
    }

    /** Envía un pedido nuevo; devuelve null si no hubo respuesta útil (queda pendiente). */
    private suspend fun intentarEnviar(request: PedidoRequestDto): PedidoResponseDto? = try {
        pedidoApi.crear(request)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    /**
     * Busca el id de Oracle de un pedido que no tiene remotoId guardado, por su
     * referencia (o por número solo en pedidos antiguos sin referencia).
     * Con [toleraSinRed] = true, un error de red devuelve null en vez de fallar.
     */
    private suspend fun buscarEnServidor(correo: String, pedido: Pedido, toleraSinRed: Boolean): Long? {
        if (correo.isBlank()) throw SesionExpiradaException()
        val remotos = try {
            pedidoApi.misPedidos(correo)
        } catch (e: IOException) {
            if (toleraSinRed) return null else throw e
        }
        val remoto = emparejar(pedido, remotos) ?: return null
        val id = remoto.id ?: return null
        pedidos.actualizar(pedido.copy(remotoId = id))
        return id
    }

    /** M9: empareja por id remoto, luego por referencia; por número solo si es un pedido antiguo. */
    private fun emparejar(local: Pedido, remotos: List<PedidoResponseDto>): PedidoResponseDto? =
        local.remotoId?.let { id -> remotos.firstOrNull { it.id == id } }
            ?: local.referencia.takeIf { it.isNotBlank() }?.let { ref ->
                remotos.firstOrNull { it.referencia == ref }
            }
            ?: if (local.referencia.isBlank() && local.remotoId == null) {
                remotos.firstOrNull { it.numero == local.numero }
            } else {
                null
            }

    /**
     * EFT (excepciones propias): cualquier falla se convierte en una subclase de
     * CasaJojoException con un mensaje claro para la pantalla, en vez de "HTTP 409".
     */
    private fun traducirError(e: Exception, enConflicto: String): CasaJojoException =
        ErroresRemotos.traducir(
            e,
            en400 = "El código QR no corresponde a la tienda",
            en404 = "No encontramos el pedido en el servidor",
            en409 = enConflicto,
        )

    private suspend fun actualizarCache(pedidoId: Long, remoto: PedidoResponseDto) {
        pedidos.porId(pedidoId)?.let { local ->
            pedidos.actualizar(local.copyEstado(remoto))
        }
    }

    private fun Pedido.copyEstado(remoto: PedidoResponseDto): Pedido = copy(
        estadoPedido = estadoLocal(remoto.estadoPedido),
        estadoPago = estadoPagoLocal(remoto.estadoPago),
        remotoId = remotoId ?: remoto.id,
    )

    private fun estadoPagoLocal(estado: String?): String = when (estado?.lowercase()) {
        "pagado" -> "pagado"
        "no_pagado" -> "no_pagado"
        else -> "pendiente"
    }

    /** EFT (POO): la conversión remoto → local la hace EstadoPedido. */
    private fun estadoLocal(estado: String?): String = EstadoPedido.desdeRemoto(estado).codigo

    companion object {
        /** M16: los pedidos de la app llegan a ms-ventas con origen "App" (antes "Mostrador"). */
        const val ORIGEN_APP = "App"

        const val MSG_PLAZO_CANCELACION = "El plazo de 15 segundos para cancelar ya terminó"
        const val MSG_NO_LISTO = "Tu pedido aún no está listo para retirar"
        const val MSG_NO_EDITABLE = "Tu pedido ya está en preparación y no se puede editar"
    }
}
