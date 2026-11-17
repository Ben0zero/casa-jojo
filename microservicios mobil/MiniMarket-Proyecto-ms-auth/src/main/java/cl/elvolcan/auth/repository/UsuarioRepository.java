package cl.elvolcan.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.elvolcan.auth.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    
    Optional<Usuario> findByUsername(String username);

    boolean existsByRut(String rut);

    boolean existsByUsername(String username);
}
