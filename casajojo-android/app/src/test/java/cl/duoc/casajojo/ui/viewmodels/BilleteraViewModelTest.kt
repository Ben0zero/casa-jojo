package cl.duoc.casajojo.ui.viewmodels

import android.app.Application
import cl.duoc.casajojo.data.repository.SinTransaccion
import cl.duoc.casajojo.data.repository.TarjetaDaoFalso
import cl.duoc.casajojo.data.repository.TarjetaRepository
import cl.duoc.casajojo.domain.usecase.MarcaTarjeta
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.YearMonth

/** Billetera: agregar, validar en vivo, predeterminada, eliminar y seguridad de los datos. */
@OptIn(ExperimentalCoroutinesApi::class)
class BilleteraViewModelTest {

    private val app = mockk<Application>(relaxed = true)
    private val dao = TarjetaDaoFalso()
    private val usuario = MutableStateFlow<Long?>(7L)
    private val hoy = { YearMonth.of(2026, 11) }

    @Before
    fun preparar() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun limpiar() = Dispatchers.resetMain()

    private fun vm() = BilleteraViewModel(app, TarjetaRepository(dao, SinTransaccion, hoy), usuario, hoy)

    private fun llenarVisa(vm: BilleteraViewModel) {
        vm.abrirFormulario()
        vm.onNumero("4242 4242 4242 4242")
        vm.onVencimiento("12/29")
        vm.onCvv("123")
        vm.onTitular("Coca Mendoza")
        vm.onAlias("Personal")
    }

    @Test
    fun `el formulario guarda solo digitos y detecta la marca`() {
        val vm = vm()
        vm.abrirFormulario()
        vm.onNumero("4242-4242 4242 4242 99999")
        vm.onVencimiento("12/295")
        vm.onCvv("12a34")
        val f = vm.estado.value.formulario
        f.numero shouldBe "4242424242424242999"
        f.vencimiento shouldBe "1229"
        f.cvv shouldBe "1234"
        f.marca shouldBe MarcaTarjeta.VISA
        vm.estado.value.mostrandoFormulario.shouldBeTrue()
    }

    @Test
    fun `agregar una tarjeta valida la guarda, cierra el formulario y borra el CVV`() {
        val vm = vm()
        llenarVisa(vm)
        vm.agregar()

        val lista = vm.tarjetas.value
        lista shouldHaveSize 1
        lista.single().etiqueta shouldBe "Visa •••• 4242"
        lista.single().predeterminada.shouldBeTrue()
        vm.estado.value.mostrandoFormulario.shouldBeFalse()
        vm.estado.value.formulario.numero shouldBe ""
        vm.estado.value.formulario.cvv shouldBe ""
        vm.estado.value.mensaje shouldBe "Tarjeta Visa •••• 4242 agregada"
    }

    @Test
    fun `nunca se guarda el numero completo ni el CVV`() {
        val vm = vm()
        llenarVisa(vm)
        vm.agregar()
        val fila = dao.filas.value.single().toString()
        fila shouldNotContain "4242424242424242"
        fila shouldNotContain "123"
    }

    @Test
    fun `con errores no guarda, marca cada campo y revalida al escribir`() {
        val vm = vm()
        vm.abrirFormulario()
        vm.onNumero("4242424242424241")
        vm.onVencimiento("0120")
        vm.agregar()

        val f = vm.estado.value.formulario
        f.intentado.shouldBeTrue()
        f.errores.numero shouldBe "El número de tarjeta no es válido"
        f.errores.vencimiento shouldBe "La tarjeta está vencida"
        f.errores.cvv.shouldNotBeNull()
        f.errores.titular.shouldNotBeNull()
        vm.estado.value.mensaje shouldBe "Revisa los campos marcados"
        dao.filas.value.shouldBeEmpty()

        vm.onNumero("4242424242424242")
        vm.estado.value.formulario.errores.numero.shouldBeNull()
    }

    @Test
    fun `marcar predeterminada y eliminar con confirmacion`() {
        val vm = vm()
        llenarVisa(vm)
        vm.agregar()
        vm.abrirFormulario()
        vm.onNumero("5555555555554444")
        vm.onVencimiento("0828")
        vm.onCvv("456")
        vm.onTitular("Coca")
        vm.agregar()

        val mc = vm.tarjetas.value.single { it.marca == "Mastercard" }
        vm.marcarPredeterminada(mc)
        vm.tarjetas.value.first().id shouldBe mc.id
        vm.estado.value.mensaje shouldBe "Mastercard •••• 4444 es ahora tu tarjeta predeterminada"

        val actual = vm.tarjetas.value.first()
        vm.pedirEliminar(actual)
        vm.estado.value.porEliminar shouldBe actual
        vm.cancelarEliminar()
        vm.estado.value.porEliminar.shouldBeNull()
        vm.tarjetas.value shouldHaveSize 2

        vm.pedirEliminar(vm.tarjetas.value.first())
        vm.confirmarEliminar()
        vm.tarjetas.value.single().marca shouldBe "Visa"
        vm.tarjetas.value.single().predeterminada.shouldBeTrue()
    }

    @Test
    fun `sin sesion no hay tarjetas ni se puede guardar`() {
        usuario.value = null
        val vm = vm()
        vm.tarjetas.value.shouldBeEmpty()
        llenarVisa(vm)
        vm.agregar()
        vm.estado.value.mensaje shouldBe "Inicia sesión para guardar tarjetas"
        dao.filas.value.shouldBeEmpty()
    }

    @Test
    fun `cerrar el formulario descarta lo escrito`() {
        val vm = vm()
        llenarVisa(vm)
        vm.cerrarFormulario()
        vm.estado.value.formulario.numero shouldBe ""
        vm.estado.value.formulario.cvv shouldBe ""
        vm.estado.value.mostrandoFormulario.shouldBeFalse()
        vm.limpiarMensaje()
        vm.estado.value.mensaje.shouldBeNull()
    }
}
