package cl.elvolcan.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity                                                      //avisa a Spring que no es una clase comun sino una entidad de bd
@Table(name = "roles")                                       // personaliza el nombre la tabla en la bd
@Data                                                        // getters and setters
@NoArgsConstructor
@AllArgsConstructor
public class Rol {
    
    @Id                                                     //esta es mi clave primaria de la tabla
    @GeneratedValue(strategy = GenerationType.IDENTITY)     //generar auto_increment el id
    private Long id;

    @NotBlank(message = "El nombre del rol no puede estar vacío")
    @Column(name = "nombre_rol", unique = true, nullable = false)
    private String nombreRol;

}
