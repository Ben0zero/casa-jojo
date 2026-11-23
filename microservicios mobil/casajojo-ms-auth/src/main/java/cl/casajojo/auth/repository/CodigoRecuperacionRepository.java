package cl.casajojo.auth.repository;

import cl.casajojo.auth.model.CodigoRecuperacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CodigoRecuperacionRepository extends JpaRepository<CodigoRecuperacion, Long> {

    /** Último código vigente (no usado) de un correo. */
    Optional<CodigoRecuperacion> findFirstByCorreoAndUsadoFalseOrderByIdDesc(String correo);

    List<CodigoRecuperacion> findByCorreoAndUsadoFalse(String correo);

    void deleteByCorreo(String correo);
}
