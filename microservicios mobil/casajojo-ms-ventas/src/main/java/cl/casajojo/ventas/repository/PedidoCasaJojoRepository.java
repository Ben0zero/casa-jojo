package cl.casajojo.ventas.repository;

import cl.casajojo.ventas.model.PedidoCasaJojo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PedidoCasaJojoRepository extends JpaRepository<PedidoCasaJojo, Long> {

    List<PedidoCasaJojo> findByClienteCorreoOrderByFechaCreacionDesc(String correo);

    Optional<PedidoCasaJojo> findByNumeroAndClienteCorreo(Integer numero, String correo);

    /** M9 / A4: pedido ya recibido con esta referencia (reenvío idempotente desde la app). */
    Optional<PedidoCasaJojo> findFirstByReferenciaAndClienteCorreo(String referencia, String correo);

    /** Cocina: pedidos de un estado, el más antiguo primero (se preparan en orden de llegada). */
    List<PedidoCasaJojo> findByEstadoPedidoOrderByFechaCreacionAsc(PedidoCasaJojo.EstadoPedido estado);

    /** Cocina: pedidos en cualquiera de los estados indicados (por defecto, los activos). */
    List<PedidoCasaJojo> findByEstadoPedidoInOrderByFechaCreacionAsc(Collection<PedidoCasaJojo.EstadoPedido> estados);
}
