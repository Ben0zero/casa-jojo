package cl.elvolcan.ventas.repository;

import cl.elvolcan.ventas.model.PedidoCasaJojo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PedidoCasaJojoRepository extends JpaRepository<PedidoCasaJojo, Long> {

    List<PedidoCasaJojo> findByClienteCorreoOrderByFechaCreacionDesc(String correo);

    Optional<PedidoCasaJojo> findByNumeroAndClienteCorreo(Integer numero, String correo);
}
