package cl.casajojo.ventas.service;

import cl.casajojo.ventas.dto.*;
import cl.casajojo.ventas.exception.ResourceNotFoundException;
import cl.casajojo.ventas.exception.TransicionInvalidaException;
import cl.casajojo.ventas.model.DetallePedidoCasaJojo;
import cl.casajojo.ventas.model.PedidoCasaJojo;
import cl.casajojo.ventas.model.PedidoCasaJojo.EstadoPedido;
import cl.casajojo.ventas.repository.PedidoCasaJojoRepository;
import cl.casajojo.ventas.security.UsuarioActual;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Pedidos de La Casa JoJo: hora de retiro, origen y estados. */
@Service
public class PedidoCasaJojoService {

    /** RF-13: plazo para cancelar, revisado también en el servidor (M2). */
    public static final Duration PLAZO_CANCELACION = Duration.ofSeconds(15);

    /**
     * M2: máquina de estados. Cualquier transición que no esté aquí → 409.
     * RECIBIDO → EN_PREPARACION → LISTO → ENTREGADO, y RECIBIDO → CANCELADO (solo dentro de 15 s).
     */
    private static final Map<EstadoPedido, Set<EstadoPedido>> TRANSICIONES = Map.of(
            EstadoPedido.RECIBIDO, EnumSet.of(EstadoPedido.EN_PREPARACION, EstadoPedido.CANCELADO),
            EstadoPedido.EN_PREPARACION, EnumSet.of(EstadoPedido.LISTO),
            EstadoPedido.LISTO, EnumSet.of(EstadoPedido.ENTREGADO),
            EstadoPedido.ENTREGADO, EnumSet.noneOf(EstadoPedido.class),
            EstadoPedido.CANCELADO, EnumSet.noneOf(EstadoPedido.class)
    );

    /** Estados que la cocina ve por defecto (pedidos todavía en curso). */
    private static final Set<EstadoPedido> ACTIVOS =
            EnumSet.of(EstadoPedido.RECIBIDO, EstadoPedido.EN_PREPARACION, EstadoPedido.LISTO);

    private final PedidoCasaJojoRepository repository;
    private final Clock reloj;
    private final String codigoRetiro;

    @Autowired
    public PedidoCasaJojoService(PedidoCasaJojoRepository repository,
                                 @Value("${casajojo.retiro.codigo:CASAJOJO-RETIRO}") String codigoRetiro) {
        this(repository, Clock.systemDefaultZone(), codigoRetiro);
    }

    /** Constructor completo: permite fijar el reloj en las pruebas del plazo de 15 s. */
    public PedidoCasaJojoService(PedidoCasaJojoRepository repository, Clock reloj, String codigoRetiro) {
        this.repository = repository;
        this.reloj = reloj;
        this.codigoRetiro = codigoRetiro;
    }

    @Transactional
    public PedidoResponseDTO crear(PedidoRequestDTO request, UsuarioActual usuario) {
        String correo = normalizar(request.getCorreo());
        // A1: un cliente solo puede crear pedidos a su propio nombre.
        usuario.exigirAcceso(correo);

        // A4 / M9: si la app reenvía un pedido que el servidor ya había guardado (por
        // ejemplo, se perdió la respuesta), se devuelve el existente y no se duplica.
        String referencia = request.getReferencia() == null || request.getReferencia().isBlank()
                ? null : request.getReferencia().trim();
        if (referencia != null) {
            var existente = repository.findFirstByReferenciaAndClienteCorreo(referencia, correo);
            if (existente.isPresent()) {
                return mapear(existente.get());
            }
        }

        // M4: el total lo calcula el servidor; si el enviado no cuadra, se rechaza.
        int totalCalculado = request.getItems().stream().mapToInt(ItemPedidoRequestDTO::getPrecio).sum();
        if (request.getTotal() == null || request.getTotal() != totalCalculado) {
            throw new IllegalArgumentException(
                    "El total enviado (" + request.getTotal() + ") no coincide con la suma de los platos ("
                            + totalCalculado + ")");
        }

        PedidoCasaJojo pedido = new PedidoCasaJojo();
        pedido.setNumero(request.getNumero() != null ? request.getNumero() : 1);
        pedido.setNombreCliente(request.getNombre().trim());
        pedido.setReferencia(referencia);
        pedido.setClienteCorreo(correo);
        pedido.setHoraRetiro(request.getHoraRetiro());
        pedido.setOrigen(request.getOrigen().trim());
        pedido.setFormaPago(request.getFormaPago() == null || request.getFormaPago().isBlank()
                ? "Transferencia" : request.getFormaPago().trim());
        pedido.setIndicaciones(request.getIndicaciones() == null ? "" : request.getIndicaciones().trim());
        pedido.setEstadoPago("pendiente");
        pedido.setTotal(totalCalculado);
        pedido.setEstadoPedido(EstadoPedido.RECIBIDO);

        for (ItemPedidoRequestDTO item : request.getItems()) {
            DetallePedidoCasaJojo detalle = new DetallePedidoCasaJojo();
            detalle.setPedido(pedido);
            detalle.setNombrePlato(item.getNombre().trim());
            detalle.setPrecio(item.getPrecio());
            detalle.setSolo(Boolean.TRUE.equals(item.getSolo()));
            detalle.setCantidad(1);
            detalle.setAcompanamientos(
                    item.getAcompanamientos() == null ? "" : String.join(", ", item.getAcompanamientos()));
            pedido.getDetalles().add(detalle);
        }

        return mapear(repository.save(pedido));
    }

    // Lecturas transaccionales: con spring.jpa.open-in-view=false, los platos (relación
    // LAZY) solo se pueden leer dentro de una transacción; sin esto respondía 500.
    @Transactional
    public List<PedidoResponseDTO> listarPorCorreo(String correo, UsuarioActual usuario) {
        String normalizado = normalizar(correo);
        usuario.exigirAcceso(normalizado); // A1: 403 si pide pedidos de otro correo
        return repository.findByClienteCorreoOrderByFechaCreacionDesc(normalizado).stream()
                .map(this::mapear)
                .collect(Collectors.toList());
    }

    @Transactional
    public PedidoResponseDTO ultimoPorCorreo(String correo, UsuarioActual usuario) {
        List<PedidoResponseDTO> pedidos = listarPorCorreo(correo, usuario);
        if (pedidos.isEmpty()) {
            throw new ResourceNotFoundException("El cliente no tiene pedidos registrados");
        }
        return pedidos.get(0);
    }

    /**
     * Cocina: GET /api/pedidos?estado=. Sin estado devuelve los pedidos activos
     * (RECIBIDO, EN_PREPARACION y LISTO), del más antiguo al más nuevo.
     * El acceso (rol COCINA o ADMIN) lo controla SecurityConfig.
     */
    @Transactional
    public List<PedidoResponseDTO> listarParaCocina(String estado) {
        List<PedidoCasaJojo> pedidos = (estado == null || estado.isBlank())
                ? repository.findByEstadoPedidoInOrderByFechaCreacionAsc(ACTIVOS)
                : repository.findByEstadoPedidoOrderByFechaCreacionAsc(parsearEstado(estado));
        return pedidos.stream().map(this::mapear).collect(Collectors.toList());
    }

    @Transactional
    public PedidoResponseDTO cambiarEstado(Long id, String estado, UsuarioActual usuario) {
        PedidoCasaJojo pedido = buscar(id);
        EstadoPedido nuevo = parsearEstado(estado);

        usuario.exigirAcceso(pedido.getClienteCorreo());
        // A1: el cliente solo puede CANCELAR su pedido; avanzar estados es tarea de la cocina.
        if (!usuario.personal() && nuevo != EstadoPedido.CANCELADO) {
            throw new AccessDeniedException("Solo la cocina puede cambiar el pedido a " + nuevo);
        }

        validarTransicion(pedido, nuevo);
        pedido.setEstadoPedido(nuevo);
        return mapear(repository.save(pedido));
    }

    /**
     * EFT / RF-12: el dueño edita los datos del retiro (nombre, hora, forma de pago e
     * indicaciones) solo mientras el pedido siga RECIBIDO. En otro estado → 409.
     */
    @Transactional
    public PedidoResponseDTO editar(Long id, PedidoEdicionDTO datos, UsuarioActual usuario) {
        PedidoCasaJojo pedido = buscar(id);
        usuario.exigirAcceso(pedido.getClienteCorreo());
        EstadoPedido actual = pedido.getEstadoPedido() == null ? EstadoPedido.RECIBIDO : pedido.getEstadoPedido();
        if (actual != EstadoPedido.RECIBIDO) {
            throw new TransicionInvalidaException(
                    "Solo se puede editar un pedido RECIBIDO (este está " + actual + ")");
        }
        pedido.setNombreCliente(datos.getNombre().trim());
        pedido.setHoraRetiro(datos.getHoraRetiro());
        pedido.setFormaPago(datos.getFormaPago().trim());
        pedido.setIndicaciones(datos.getIndicaciones() == null ? "" : datos.getIndicaciones().trim());
        return mapear(repository.save(pedido));
    }

    /** Caja (COCINA/ADMIN): marca el pago. Un pedido cancelado no se puede pagar. */
    @Transactional
    public PedidoResponseDTO marcarPagado(Long id) {
        PedidoCasaJojo pedido = buscar(id);
        if (pedido.getEstadoPedido() == EstadoPedido.CANCELADO) {
            throw new TransicionInvalidaException("No se puede pagar un pedido cancelado");
        }
        pedido.setEstadoPago("pagado");
        return mapear(repository.save(pedido));
    }

    /**
     * A5 / RF-14: retiro con el QR de la tienda. La app envía el código leído del QR
     * (casajojo://retiro?codigo=...) y el servidor lo compara con RETIRO_CODIGO.
     * Si es correcto y el pedido está LISTO, queda pagado y ENTREGADO en un solo paso.
     */
    @Transactional
    public PedidoResponseDTO retirar(Long id, String codigo, UsuarioActual usuario) {
        PedidoCasaJojo pedido = buscar(id);
        usuario.exigirAcceso(pedido.getClienteCorreo());

        if (codigo == null || !MessageDigest.isEqual(
                codigo.trim().getBytes(StandardCharsets.UTF_8),
                codigoRetiro.getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("El código QR no corresponde a la tienda");
        }
        validarTransicion(pedido, EstadoPedido.ENTREGADO);
        pedido.setEstadoPago("pagado");
        pedido.setEstadoPedido(EstadoPedido.ENTREGADO);
        return mapear(repository.save(pedido));
    }

    /** M2: 409 si la transición no está permitida o si la cancelación llega tarde. */
    private void validarTransicion(PedidoCasaJojo pedido, EstadoPedido nuevo) {
        EstadoPedido actual = pedido.getEstadoPedido() == null ? EstadoPedido.RECIBIDO : pedido.getEstadoPedido();
        if (!TRANSICIONES.get(actual).contains(nuevo)) {
            throw new TransicionInvalidaException(
                    "No se puede pasar el pedido de " + actual + " a " + nuevo);
        }
        if (nuevo == EstadoPedido.CANCELADO) {
            LocalDateTime creado = pedido.getFechaCreacion();
            if (creado == null
                    || Duration.between(creado, LocalDateTime.now(reloj)).compareTo(PLAZO_CANCELACION) > 0) {
                throw new TransicionInvalidaException(
                        "El plazo de " + PLAZO_CANCELACION.toSeconds() + " segundos para cancelar ya terminó");
            }
        }
    }

    private PedidoCasaJojo buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado: " + id));
    }

    /** 400 (IllegalArgumentException) si el estado no existe. */
    private EstadoPedido parsearEstado(String estado) {
        try {
            return EstadoPedido.valueOf(estado.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Estado de pedido inválido: " + estado);
        }
    }

    private String normalizar(String correo) {
        return correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
    }

    private PedidoResponseDTO mapear(PedidoCasaJojo p) {
        List<ItemPedidoResponseDTO> items = p.getDetalles() == null
                ? Collections.emptyList()
                : p.getDetalles().stream()
                .map(d -> new ItemPedidoResponseDTO(
                        d.getNombrePlato(),
                        d.getPrecio(),
                        Boolean.TRUE.equals(d.getSolo()),
                        d.getAcompanamientos() == null || d.getAcompanamientos().isBlank()
                                ? Collections.emptyList()
                                : Arrays.asList(d.getAcompanamientos().split(", ")),
                        d.getCantidad()))
                .collect(Collectors.toList());

        return new PedidoResponseDTO(
                p.getId(), p.getNumero(), p.getNombreCliente(), p.getClienteCorreo(),
                p.getHoraRetiro(), p.getOrigen(), p.getFormaPago(), p.getIndicaciones(),
                p.getEstadoPedido() == null ? "RECIBIDO" : p.getEstadoPedido().name(),
                p.getEstadoPago(), p.getTotal(), p.getFechaCreacion(), items, p.getReferencia()
        );
    }
}
