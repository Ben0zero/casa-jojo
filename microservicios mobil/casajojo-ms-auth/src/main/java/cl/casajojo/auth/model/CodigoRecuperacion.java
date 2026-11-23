package cl.casajojo.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * EFT: código de un solo uso para restablecer la contraseña.
 * Tabla propia (no se agregan columnas a "usuarios", que es compartida en Oracle).
 * El código NUNCA se guarda en texto plano: solo su hash BCrypt.
 */
@Entity
@Table(name = "cj_codigos_recuperacion")
@Data
@NoArgsConstructor
public class CodigoRecuperacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String correo;

    @Column(name = "codigo_hash", nullable = false, length = 100)
    private String codigoHash;

    @Column(nullable = false)
    private LocalDateTime expira;

    @Column(nullable = false)
    private boolean usado;

    /** Intentos fallidos: al llegar al máximo el código queda inutilizado. */
    @Column(nullable = false)
    private int intentos;
}
