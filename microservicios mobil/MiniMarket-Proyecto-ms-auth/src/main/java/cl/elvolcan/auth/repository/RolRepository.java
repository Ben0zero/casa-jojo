package cl.elvolcan.auth.repository;


import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;


import cl.elvolcan.auth.model.Rol;


public interface RolRepository extends JpaRepository<Rol, Long>{
    // extends JpaRepo obtiene los 10 metodos de bd .saveAll findAll etc
    // el <Rol, Long> son dos instrucciones precisas para que funcione, la tabla Rol y busque el tipo de dato Long que es la llave primaria



    //Optional ayuda que al buscar un rol que no exista, devuelva un null en vez de un error(nullpointerexception)
    Optional<Rol> findByNombreRol(String nombreRol);

}
