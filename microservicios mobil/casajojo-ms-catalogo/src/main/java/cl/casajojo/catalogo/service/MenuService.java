package cl.casajojo.catalogo.service;

import cl.casajojo.catalogo.dto.PlatoMenuDTO;
import java.util.List;

/** Servicio del menú de La Casa JoJo. */
public interface MenuService {
    List<PlatoMenuDTO> obtenerMenu();

    PlatoMenuDTO obtenerPorId(Long id);

    /** Cocina/Admin: marca un plato como disponible o agotado (404 si no es del menú). */
    PlatoMenuDTO cambiarDisponibilidad(Long id, boolean disponible);
}
