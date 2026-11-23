package cl.casajojo.ventas.service;

import cl.casajojo.ventas.dto.ItemPedidoRequestDTO;
import cl.casajojo.ventas.dto.PedidoRequestDTO;
import cl.casajojo.ventas.dto.PedidoResponseDTO;
import cl.casajojo.ventas.exception.TransicionInvalidaException;
import cl.casajojo.ventas.model.PedidoCasaJojo;
import cl.casajojo.ventas.model.PedidoCasaJojo.EstadoPedido;
import cl.casajojo.ventas.repository.PedidoCasaJojoRepository;
import cl.casajojo.ventas.security.UsuarioActual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.access.AccessDeniedException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Pruebas del servicio de pedidos de La Casa JoJo (A1, M2, M4, A5). */
class PedidoCasaJojoServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/Santiago");
    /** "Ahora" fijo para probar el plazo de 15 s sin depender del reloj real. */
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 23, 13, 0, 0);
    private static final String CODIGO = "CODIGO-TIENDA";

    private static final UsuarioActual CAMILA = new UsuarioActual("camila@casajojo.cl", false);
    private static final UsuarioActual OTRO = new UsuarioActual("otro@casajojo.cl", false);
    private static final UsuarioActual COCINA = new UsuarioActual("cocina@casajojo.cl", true);

    private PedidoCasaJojoRepository repository;
    private PedidoCasaJojoService service;

    @BeforeEach
    void setUp() {
        repository = mock(PedidoCasaJojoRepository.class);
        Clock reloj = Clock.fixed(AHORA.atZone(ZONA).toInstant(), ZONA);
        service = new PedidoCasaJojoService(repository, reloj, CODIGO);
        when(repository.save(any(PedidoCasaJojo.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private PedidoRequestDTO request() {
        ItemPedidoRequestDTO item = new ItemPedidoRequestDTO();
        item.setNombre("Pollo asado");
        item.setPrecio(4800);
        item.setSolo(false);
        item.setAcompanamientos(List.of("Arroz", "Ensalada"));

        PedidoRequestDTO request = new PedidoRequestDTO();
        request.setNumero(7);
        request.setNombre("Camila González");
        request.setCorreo(" CAMILA@CASAJOJO.CL ");
        request.setHoraRetiro("13:00");
        request.setOrigen("Mostrador");
        request.setFormaPago("Transferencia");
        request.setIndicaciones("Sin cebolla");
        request.setItems(List.of(item));
        request.setTotal(4800);
        return request;
    }

    private PedidoCasaJojo pedido(EstadoPedido estado, LocalDateTime creado) {
        PedidoCasaJojo p = new PedidoCasaJojo();
        p.setId(5L);
        p.setClienteCorreo("camila@casajojo.cl");
        p.setEstadoPedido(estado);
        p.setEstadoPago("pendiente");
        p.setFechaCreacion(creado);
        when(repository.findById(5L)).thenReturn(Optional.of(p));
        return p;
    }

    // ---------- crear (A1 + M4) ----------

    @Test
    @DisplayName("crear guarda el pedido con estado RECIBIDO, pago pendiente y sus platos")
    void crearPedidoCasaJojo() {
        PedidoResponseDTO resultado = service.crear(request(), CAMILA);

        assertEquals(7, resultado.getNumero());
        assertEquals("camila@casajojo.cl", resultado.getCorreo());
        assertEquals("RECIBIDO", resultado.getEstadoPedido());
        assertEquals("pendiente", resultado.getEstadoPago());
        assertEquals(4800, resultado.getTotal());
        assertEquals(List.of("Arroz", "Ensalada"), resultado.getItems().get(0).getAcompanamientos());
    }

    @Test
    @DisplayName("A1: un cliente no puede crear un pedido a nombre de otro correo (403)")
    void crearConCorreoAjeno403() {
        assertThrows(AccessDeniedException.class, () -> service.crear(request(), OTRO));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("A1: la cocina sí puede registrar pedidos para cualquier correo")
    void cocinaCreaParaOtroCorreo() {
        assertEquals("RECIBIDO", service.crear(request(), COCINA).getEstadoPedido());
    }

    @Test
    @DisplayName("M4: si el total no coincide con la suma de los platos → IllegalArgumentException (400)")
    void totalQueNoCuadra400() {
        PedidoRequestDTO req = request();
        req.setTotal(100);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> service.crear(req, CAMILA));
        assertTrue(e.getMessage().contains("no coincide"));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("A4/M9: reenviar un pedido con la misma referencia devuelve el existente y no duplica")
    void reenvioIdempotente() {
        PedidoCasaJojo existente = new PedidoCasaJojo();
        existente.setId(77L);
        existente.setNumero(7);
        existente.setClienteCorreo("camila@casajojo.cl");
        existente.setEstadoPedido(EstadoPedido.RECIBIDO);
        existente.setReferencia("ref-12345678");
        when(repository.findFirstByReferenciaAndClienteCorreo("ref-12345678", "camila@casajojo.cl"))
                .thenReturn(Optional.of(existente));

        PedidoRequestDTO req = request();
        req.setReferencia("ref-12345678");
        PedidoResponseDTO r = service.crear(req, CAMILA);

        assertEquals(77L, r.getId());
        assertEquals("ref-12345678", r.getReferencia());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("M9: un pedido nuevo guarda su referencia y la devuelve")
    void pedidoNuevoGuardaReferencia() {
        when(repository.findFirstByReferenciaAndClienteCorreo(any(), any())).thenReturn(Optional.empty());
        PedidoRequestDTO req = request();
        req.setReferencia("ref-nueva-001");

        assertEquals("ref-nueva-001", service.crear(req, CAMILA).getReferencia());
        verify(repository).save(any());
    }

    // ---------- consultas (A1) ----------

    @Test
    @DisplayName("listarPorCorreo normaliza el correo antes de consultar")
    void listarPorCorreoNormalizado() {
        when(repository.findByClienteCorreoOrderByFechaCreacionDesc("camila@casajojo.cl")).thenReturn(List.of());

        service.listarPorCorreo(" Camila@Casajojo.cl ", CAMILA);

        verify(repository).findByClienteCorreoOrderByFechaCreacionDesc("camila@casajojo.cl");
    }

    @Test
    @DisplayName("A1: pedir los pedidos de otro correo → 403")
    void listarDeOtroCorreo403() {
        assertThrows(AccessDeniedException.class, () -> service.listarPorCorreo("camila@casajojo.cl", OTRO));
        verify(repository, never()).findByClienteCorreoOrderByFechaCreacionDesc(any());
    }

    @Test
    @DisplayName("Cocina: sin estado devuelve los activos; con estado inválido → 400")
    void listarParaCocina() {
        when(repository.findByEstadoPedidoInOrderByFechaCreacionAsc(any())).thenReturn(List.of());
        service.listarParaCocina(null);
        verify(repository).findByEstadoPedidoInOrderByFechaCreacionAsc(
                java.util.EnumSet.of(EstadoPedido.RECIBIDO, EstadoPedido.EN_PREPARACION, EstadoPedido.LISTO));

        when(repository.findByEstadoPedidoOrderByFechaCreacionAsc(EstadoPedido.LISTO)).thenReturn(List.of());
        service.listarParaCocina("listo");
        verify(repository).findByEstadoPedidoOrderByFechaCreacionAsc(EstadoPedido.LISTO);

        assertThrows(IllegalArgumentException.class, () -> service.listarParaCocina("FOO"));
    }

    // ---------- máquina de estados (M2) ----------

    @ParameterizedTest(name = "{0} → {1} permitido")
    @CsvSource({"RECIBIDO,EN_PREPARACION", "EN_PREPARACION,LISTO", "LISTO,ENTREGADO"})
    @DisplayName("M2: la cocina avanza el pedido por el flujo normal")
    void transicionesValidas(EstadoPedido desde, EstadoPedido hacia) {
        pedido(desde, AHORA.minusMinutes(5));
        assertEquals(hacia.name(), service.cambiarEstado(5L, hacia.name(), COCINA).getEstadoPedido());
    }

    @ParameterizedTest(name = "{0} → {1} rechazado")
    @CsvSource({"ENTREGADO,RECIBIDO", "CANCELADO,LISTO", "RECIBIDO,LISTO", "LISTO,EN_PREPARACION",
            "ENTREGADO,CANCELADO", "EN_PREPARACION,CANCELADO", "LISTO,LISTO"})
    @DisplayName("M2: cualquier otra transición → TransicionInvalidaException (409)")
    void transicionesInvalidas(EstadoPedido desde, EstadoPedido hacia) {
        pedido(desde, AHORA.minusSeconds(1));
        assertThrows(TransicionInvalidaException.class, () -> service.cambiarEstado(5L, hacia.name(), COCINA));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("M2: el cliente cancela dentro de 15 s")
    void cancelarDentroDelPlazo() {
        pedido(EstadoPedido.RECIBIDO, AHORA.minusSeconds(10));
        assertEquals("CANCELADO", service.cambiarEstado(5L, "CANCELADO", CAMILA).getEstadoPedido());
    }

    @Test
    @DisplayName("M2: cancelar después de 15 s → 409")
    void cancelarFueraDePlazo409() {
        pedido(EstadoPedido.RECIBIDO, AHORA.minusSeconds(16));
        TransicionInvalidaException e = assertThrows(TransicionInvalidaException.class,
                () -> service.cambiarEstado(5L, "CANCELADO", CAMILA));
        assertTrue(e.getMessage().contains("15 segundos"));
    }

    @Test
    @DisplayName("A1: el cliente no puede avanzar su pedido a LISTO (403) ni tocar pedidos ajenos (403)")
    void clienteNoAvanzaEstados() {
        pedido(EstadoPedido.EN_PREPARACION, AHORA.minusMinutes(1));
        assertThrows(AccessDeniedException.class, () -> service.cambiarEstado(5L, "LISTO", CAMILA));
        assertThrows(AccessDeniedException.class, () -> service.cambiarEstado(5L, "CANCELADO", OTRO));
    }

    @Test
    @DisplayName("Estado inexistente → 400")
    void estadoInvalido() {
        pedido(EstadoPedido.RECIBIDO, AHORA);
        assertThrows(IllegalArgumentException.class, () -> service.cambiarEstado(5L, "FOO", COCINA));
    }

    @Test
    @DisplayName("Pagar un pedido cancelado → 409")
    void pagarCancelado409() {
        pedido(EstadoPedido.CANCELADO, AHORA);
        assertThrows(TransicionInvalidaException.class, () -> service.marcarPagado(5L));
    }

    // ---------- retiro con QR (A5) ----------

    @Test
    @DisplayName("A5: código correcto y pedido LISTO → pagado y ENTREGADO")
    void retirarConCodigoCorrecto() {
        pedido(EstadoPedido.LISTO, AHORA.minusMinutes(20));
        PedidoResponseDTO r = service.retirar(5L, " CODIGO-TIENDA ", CAMILA);
        assertEquals("ENTREGADO", r.getEstadoPedido());
        assertEquals("pagado", r.getEstadoPago());
    }

    @Test
    @DisplayName("A5: código incorrecto → 400; pedido aún no LISTO → 409; pedido ajeno → 403")
    void retirarRechazos() {
        pedido(EstadoPedido.LISTO, AHORA.minusMinutes(20));
        assertThrows(IllegalArgumentException.class, () -> service.retirar(5L, "https://otra.cosa", CAMILA));
        assertThrows(AccessDeniedException.class, () -> service.retirar(5L, CODIGO, OTRO));

        pedido(EstadoPedido.EN_PREPARACION, AHORA.minusMinutes(20));
        assertThrows(TransicionInvalidaException.class, () -> service.retirar(5L, CODIGO, CAMILA));
    }

    // ---------- EFT / RF-12: editar pedido mientras está RECIBIDO ----------

    private cl.casajojo.ventas.dto.PedidoEdicionDTO edicion() {
        var d = new cl.casajojo.ventas.dto.PedidoEdicionDTO();
        d.setNombre("  Camila R. ");
        d.setHoraRetiro("14:00");
        d.setFormaPago("Pagar en tienda");
        d.setIndicaciones(" Sin ají ");
        return d;
    }

    @Test
    @DisplayName("RF-12: el dueño edita un pedido RECIBIDO")
    void editarRecibido() {
        pedido(EstadoPedido.RECIBIDO, AHORA.minusMinutes(5));
        PedidoResponseDTO r = service.editar(5L, edicion(), CAMILA);
        assertEquals("Camila R.", r.getNombre());
        assertEquals("14:00", r.getHoraRetiro());
        assertEquals("Pagar en tienda", r.getFormaPago());
        assertEquals("Sin ají", r.getIndicaciones());
    }

    @Test
    @DisplayName("RF-12: editar un pedido EN_PREPARACION responde 409")
    void editarEnPreparacion409() {
        pedido(EstadoPedido.EN_PREPARACION, AHORA.minusMinutes(5));
        assertThrows(TransicionInvalidaException.class, () -> service.editar(5L, edicion(), CAMILA));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("RF-12: editar el pedido de otro cliente responde 403")
    void editarAjeno403() {
        pedido(EstadoPedido.RECIBIDO, AHORA.minusMinutes(5));
        assertThrows(AccessDeniedException.class, () -> service.editar(5L, edicion(), OTRO));
    }
}
