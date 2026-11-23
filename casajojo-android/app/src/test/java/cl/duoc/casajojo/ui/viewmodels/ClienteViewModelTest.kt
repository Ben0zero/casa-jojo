package cl.duoc.casajojo.ui.viewmodels

import android.app.Application
import cl.duoc.casajojo.data.local.SesionPreferences
import cl.duoc.casajojo.data.local.entity.Pedido
import cl.duoc.casajojo.data.local.entity.PedidoWithPlatos
import cl.duoc.casajojo.data.local.entity.Producto
import cl.duoc.casajojo.data.local.entity.Usuario
import cl.duoc.casajojo.data.remote.dto.PedidoResponseDto
import cl.duoc.casajojo.data.repository.AuthRepository
import cl.duoc.casajojo.data.repository.NutricionRepository
import cl.duoc.casajojo.domain.model.NutricionItem
import cl.duoc.casajojo.domain.model.NutricionPlato
import cl.duoc.casajojo.data.repository.PedidoRepository
import cl.duoc.casajojo.domain.error.ConflictoException
import cl.duoc.casajojo.domain.error.ValidacionException
import cl.duoc.casajojo.domain.model.PlatoDraft
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalTime

/**
 * EFT (Fase F): ViewModel del flujo del cliente con repositorios, DataStore y
 * notificaciones mockeados (ver el constructor inyectable de ClienteViewModel).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ClienteViewModelTest {

    private val app = mockk<Application>(relaxed = true)
    private val repo = mockk<PedidoRepository>(relaxed = true)
    private val sesion = mockk<SesionPreferences>(relaxed = true)
    private val authRepo = mockk<AuthRepository>(relaxed = true)
    private val avisos = mockk<AvisosPedido>(relaxed = true)
    private val nutricion = mockk<NutricionRepository>()
    private var hora = LocalTime.of(11, 0)

    private val pollo = Producto(1, "Pollo asado", 4800, Producto.TIPO_PROTEINA)
    private val cerdo = Producto(4, "Cerdo", 4600, Producto.TIPO_PROTEINA, disponible = false)
    private val arroz = Producto(7, "Arroz", 0, Producto.TIPO_ACOMPANAMIENTO)

    private fun pedido(estado: String = "recibido") = Pedido(
        id = 3L, numero = 7, nombre = "Camila", horaRetiro = "13:00", formaPago = "Transferencia",
        indicaciones = "", estadoPedido = estado, estadoPago = "pendiente", total = 4800, fecha = 0L,
        usuarioId = 11L, remotoId = 100L,
    )

    @Before
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { sesion.usuarioId } returns flowOf(11L)
        coEvery { sesion.ultimoCorreo() } returns "camila@casajojo.cl"
        every { authRepo.usuarioActual } returns
            flowOf(Usuario(11L, "Camila", "camila@casajojo.cl", "912345678", "h", "s"))
        every { repo.proteinas } returns flowOf(listOf(pollo, cerdo))
        every { repo.acompanamientos } returns flowOf(listOf(arroz))
        every { repo.ultimoPedidoDe(11L) } returns flowOf(PedidoWithPlatos(pedido()))
        every { repo.pedidosDe(11L) } returns flowOf(listOf(PedidoWithPlatos(pedido())))
        every { repo.historialDe(11L, any()) } answers {
            flowOf(listOf(PedidoWithPlatos(pedido(secondArg()))))
        }
    }

    @After
    fun limpiar() = Dispatchers.resetMain()

    private fun vm() = ClienteViewModel(app, repo, sesion, authRepo, avisos, ahora = { hora }, nutricionRepo = nutricion)

    /** Activa un StateFlow con WhileSubscribed y devuelve su valor actual. */
    private fun <T> observar(flujo: Flow<T>, valor: () -> T): T {
        val job = CoroutineScope(UnconfinedTestDispatcher()).launch { flujo.collect {} }
        return valor().also { job.cancel() }
    }

    private fun armarCarrito(vm: ClienteViewModel) {
        vm.elegirProteina(pollo)
        vm.toggleAcompanamiento("Arroz")
        vm.agregarPlato().shouldBeTrue()
    }

    @Test
    fun `al abrir sincroniza el menu y los pedidos del usuario`() {
        val vm = vm()
        coVerify { repo.sincronizarMenu() }
        coVerify { repo.sincronizarEstados(11L, "camila@casajojo.cl") }
        vm.sincronizarConServidor()
        vm.sincronizarUltimoPedido()
        coVerify(atLeast = 3) { repo.sincronizarEstados(11L, any()) }
    }

    @Test
    fun `expone menu, usuario y pedidos desde Room`() {
        val vm = vm()
        observar(vm.proteinas) { vm.proteinas.value } shouldBe listOf(pollo, cerdo)
        observar(vm.acompanamientos) { vm.acompanamientos.value } shouldBe listOf(arroz)
        observar(vm.usuario) { vm.usuario.value }?.nombre shouldBe "Camila"
        observar(vm.ultimoPedido) { vm.ultimoPedido.value }?.pedido?.numero shouldBe 7
        observar(vm.misPedidos) { vm.misPedidos.value }.size shouldBe 1
    }

    @Test
    fun `el historial sigue el filtro elegido`() {
        val vm = vm()
        observar(vm.historial) { vm.historial.value }.pedidos.first().pedido.estadoPedido shouldBe "entregado"
        vm.onFiltroHistorial(FiltroHistorial.Cancelados)
        vm.filtroHistorial.value shouldBe FiltroHistorial.Cancelados
        val estado = observar(vm.historial) { vm.historial.value }
        estado.filtro shouldBe FiltroHistorial.Cancelados
        estado.pedidos.first().pedido.estadoPedido shouldBe "cancelado"
    }

    @Test
    fun `solo proteina agrega el plato a 3000 y las agotadas se ignoran`() {
        val vm = vm()
        vm.elegirProteina(cerdo)
        vm.cart.shouldBeEmpty()
        vm.toggleSolo()
        vm.solo.shouldBeTrue()
        vm.elegirProteina(pollo)
        vm.cart shouldBe listOf(PlatoDraft("Pollo asado", 3000, solo = true))
        vm.solo.shouldBeFalse()
        vm.total shouldBe 3000
    }

    @Test
    fun `un plato normal exige acompañamiento`() {
        val vm = vm()
        vm.agregarPlato().shouldBeFalse() // sin proteína elegida
        vm.elegirProteina(pollo)
        vm.proteinaSeleccionada shouldBe pollo
        vm.agregarPlato().shouldBeFalse()
        vm.error shouldBe "Elige al menos un acompañamiento"
        vm.limpiarError()
        vm.error.shouldBeNull()

        vm.toggleAcompanamiento("Arroz")
        vm.toggleAcompanamiento("Puré")
        vm.toggleAcompanamiento("Puré")
        vm.acompanamientosSel shouldBe listOf("Arroz")
        vm.agregarPlato().shouldBeTrue()
        vm.cart.single().precio shouldBe 4800

        vm.quitarPlato(5) // índice inválido: no hace nada
        vm.cart.size shouldBe 1
        vm.quitarPlato(0)
        vm.cart.shouldBeEmpty()
    }

    @Test
    fun `confirmar sin platos o sin nombre muestra el error`() {
        val vm = vm()
        vm.confirmarPedido {}
        vm.error shouldBe "Agrega al menos un plato a tu pedido"

        armarCarrito(vm)
        vm.confirmarPedido {}
        vm.retiro.errores.nombre shouldBe "Escribe tu nombre para el retiro"
        vm.error shouldBe "Revisa los campos marcados"

        // Tras el primer intento, el nombre se valida mientras se escribe.
        vm.onNombre("Ana")
        vm.retiro.errores.nombre.shouldBeNull()
        coVerify(exactly = 0) { repo.registrarPedido(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `no acepta una hora de retiro que ya paso`() {
        hora = LocalTime.of(14, 10)
        val vm = vm()
        armarCarrito(vm)
        vm.onNombre("Ana")
        vm.onHoraRetiro("13:00")
        vm.confirmarPedido {}
        vm.error shouldBe "Elige una hora de retiro que aún no haya pasado"
    }

    @Test
    fun `confirmar registra el pedido, avisa y deja el carrito listo`() {
        coEvery { repo.registrarPedido(any(), any(), any(), any(), any(), any(), any(), any()) } returns 8
        val vm = vm()
        armarCarrito(vm)
        vm.prellenarNombre("Camila")
        vm.prellenarNombre("Otro") // no pisa lo ya escrito
        vm.onHoraRetiro("13:30")
        vm.onFormaPago("Pagar en tienda")
        vm.onIndicaciones(" sin ají ")
        var navego = false
        vm.confirmarPedido { navego = true }

        navego.shouldBeTrue()
        vm.ultimoNumero shouldBe 8
        vm.cart.shouldBeEmpty()
        vm.enRegistro.shouldBeFalse()
        verify { avisos.pedidoRegistrado(8, "13:30") }
        coVerify {
            repo.registrarPedido(
                11L, "Camila", "13:30", "Pagar en tienda", "sin ají", any(), "camila@casajojo.cl",
                PedidoRepository.ORIGEN_APP,
            )
        }
    }

    @Test
    fun `si Room falla al registrar se conserva el carrito`() {
        coEvery { repo.registrarPedido(any(), any(), any(), any(), any(), any(), any(), any()) } throws
            IllegalStateException("db")
        val vm = vm()
        armarCarrito(vm)
        vm.onNombre("Ana")
        vm.confirmarPedido {}

        vm.error shouldBe "No pudimos registrar tu pedido. Intenta de nuevo."
        vm.cart.size shouldBe 1
        vm.enRegistro.shouldBeFalse()
    }

    @Test
    fun `cancelar avisa el resultado y quita la notificacion`() {
        coEvery { repo.cancelarPedidoRemoto(any(), any()) } returns Result.success(PedidoResponseDto())
        val vm = vm()
        var ok: Boolean? = null
        vm.cancelarPedido(pedido()) { ok = it }
        ok shouldBe true
        verify { avisos.quitarPedidoRegistrado() }

        coEvery { repo.cancelarPedidoRemoto(any(), any()) } returns
            Result.failure(ConflictoException(PedidoRepository.MSG_PLAZO_CANCELACION))
        vm.cancelarPedido(pedido()) { ok = it }
        ok shouldBe false
        vm.error shouldBe PedidoRepository.MSG_PLAZO_CANCELACION
        vm.cancelando.shouldBeFalse()
    }

    @Test
    fun `retiro con QR valida estado y codigo antes de llamar al servidor`() {
        val vm = vm()
        vm.registrarRetiro(pedido("recibido"), "casajojo://retiro?codigo=CASAJOJO-RETIRO")
        vm.error shouldBe "Tu pedido aún no está listo para retirar"

        vm.registrarRetiro(pedido("listo"), "https://otro.cl")
        vm.error shouldBe "Este QR no es de La Casa JoJo"
        coVerify(exactly = 0) { repo.marcarEntregadoRemoto(any(), any(), any()) }

        coEvery { repo.marcarEntregadoRemoto(any(), any(), any()) } returns
            Result.failure(ValidacionException("El código QR no corresponde a la tienda"))
        vm.registrarRetiro(pedido("listo"), "casajojo://retiro?codigo=CASAJOJO-RETIRO")
        coVerify { repo.marcarEntregadoRemoto("camila@casajojo.cl", any(), "CASAJOJO-RETIRO") }
        vm.error shouldBe "El código QR no corresponde a la tienda"
    }

    @Test
    fun `editar pedido abre la ficha con los datos y guarda los cambios`() {
        coEvery { repo.editarPedido(any(), any(), any(), any(), any(), any()) } returns Result.success(pedido())
        val vm = vm()
        vm.abrirEdicion(pedido("listo"))
        vm.edicion.abierta.shouldBeFalse()
        vm.error shouldBe PedidoRepository.MSG_NO_EDITABLE

        vm.abrirEdicion(pedido())
        vm.edicion.abierta.shouldBeTrue()
        vm.edicion.nombre shouldBe "Camila"
        vm.onEdicionNombre("")
        var aviso: String? = null
        vm.guardarEdicion { aviso = it }
        vm.edicion.errorNombre shouldBe "Escribe tu nombre para el retiro"

        vm.onEdicionNombre("Cami")
        vm.onEdicionHora("14:00")
        vm.onEdicionFormaPago("Pagar en tienda")
        vm.onEdicionIndicaciones("Sin ají")
        vm.guardarEdicion { aviso = it }

        aviso shouldBe "Pedido #7 actualizado"
        vm.edicion.abierta.shouldBeFalse()
        coVerify { repo.editarPedido("camila@casajojo.cl", any(), "Cami", "14:00", "Pagar en tienda", "Sin ají") }
    }

    @Test
    fun `si la cocina ya empezo la edicion se cierra con el aviso`() {
        coEvery { repo.editarPedido(any(), any(), any(), any(), any(), any()) } returns
            Result.failure(ConflictoException(PedidoRepository.MSG_NO_EDITABLE))
        val vm = vm()
        vm.abrirEdicion(pedido())
        var aviso: String? = null
        vm.guardarEdicion { aviso = it }
        aviso shouldBe PedidoRepository.MSG_NO_EDITABLE
        vm.edicion.abierta.shouldBeFalse()
    }

    @Test
    fun `un nombre rechazado mantiene la ficha abierta con el error`() {
        coEvery { repo.editarPedido(any(), any(), any(), any(), any(), any()) } returns
            Result.failure(ValidacionException("Nombre inválido", campo = "nombre"))
        val vm = vm()
        vm.abrirEdicion(pedido())
        vm.guardarEdicion {}
        vm.edicion.abierta.shouldBeTrue()
        vm.edicion.errorNombre shouldBe "Nombre inválido"
        vm.edicion.guardando.shouldBeFalse()
        vm.cerrarEdicion()
        vm.edicion.abierta.shouldBeFalse()
    }

    @Test
    fun `limpiar todo descarta el carrito, el numero y la edicion`() {
        coEvery { repo.registrarPedido(any(), any(), any(), any(), any(), any(), any(), any()) } returns 8
        val vm = vm()
        armarCarrito(vm)
        vm.abrirEdicion(pedido())
        vm.limpiarTodo()
        vm.cart.shouldBeEmpty()
        vm.ultimoNumero.shouldBeNull()
        vm.edicion.abierta.shouldBeFalse()
    }

    // ---------- Nutrición con Open Food Facts (API externa) ----------

    private val platoPollo = PlatoDraft("Pollo asado", 4800, solo = false, acompanamientos = listOf("Arroz"))
    private val datosPollo = NutricionPlato(
        items = listOf(NutricionItem("Pollo asado", "Pollo", 200.0, 25.0, 0.0, 10.0)),
        total = NutricionItem("Total", "", 200.0, 25.0, 0.0, 10.0),
    )

    @Test
    fun `mostrar nutricion consulta una sola vez y guarda los datos`() {
        coEvery { nutricion.nutricionDe(platoPollo) } returns Result.success(datosPollo)
        val vm = vm()

        vm.mostrarNutricion(platoPollo)
        val estado = vm.nutricionPorPlato.getValue(platoPollo)
        estado.expandido.shouldBeTrue()
        estado.cargando.shouldBeFalse()
        estado.datos shouldBe datosPollo
        estado.error.shouldBeNull()

        vm.ocultarNutricion(platoPollo)
        vm.nutricionPorPlato.getValue(platoPollo).expandido.shouldBeFalse()
        vm.mostrarNutricion(platoPollo) // ya tiene datos: no vuelve a consultar
        coVerify(exactly = 1) { nutricion.nutricionDe(platoPollo) }
    }

    @Test
    fun `si Open Food Facts falla muestra el error y reintentar vuelve a consultar`() {
        coEvery { nutricion.nutricionDe(platoPollo) } returnsMany listOf(
            Result.failure(IllegalStateException("sin datos")),
            Result.success(datosPollo),
        )
        val vm = vm()

        vm.mostrarNutricion(platoPollo)
        vm.nutricionPorPlato.getValue(platoPollo).error shouldBe "No se pudo consultar la nutrición"

        vm.reintentarNutricion(platoPollo)
        vm.nutricionPorPlato.getValue(platoPollo).datos shouldBe datosPollo
        vm.nutricionPorPlato.getValue(platoPollo).error.shouldBeNull()
        coVerify(exactly = 2) { nutricion.nutricionDe(platoPollo) }
    }

    @Test
    fun `una excepcion inesperada del repositorio tambien se muestra como error`() {
        coEvery { nutricion.nutricionDe(any()) } throws IllegalArgumentException("x")
        val vm = vm()
        vm.mostrarNutricion(platoPollo)
        vm.nutricionPorPlato.getValue(platoPollo).error shouldBe "No se pudo consultar la nutrición"
    }

    @Test
    fun `ocultar un plato sin consulta no hace nada`() {
        val vm = vm()
        vm.ocultarNutricion(platoPollo)
        vm.nutricionPorPlato shouldBe emptyMap()
    }

    @Test
    fun `quitar un plato o limpiar todo descarta su nutricion`() {
        coEvery { nutricion.nutricionDe(any()) } returns Result.success(datosPollo)
        val vm = vm()
        armarCarrito(vm)
        val plato = vm.cart.single()
        vm.mostrarNutricion(plato)
        vm.nutricionPorPlato.containsKey(plato).shouldBeTrue()

        vm.quitarPlato(0)
        vm.nutricionPorPlato.containsKey(plato).shouldBeFalse()

        armarCarrito(vm)
        vm.mostrarNutricion(vm.cart.single())
        vm.limpiarTodo()
        vm.nutricionPorPlato shouldBe emptyMap()
    }
}
