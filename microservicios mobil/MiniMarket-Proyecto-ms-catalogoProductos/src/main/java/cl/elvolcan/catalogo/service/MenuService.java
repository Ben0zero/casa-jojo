package cl.elvolcan.catalogo.service;

import cl.elvolcan.catalogo.dto.PlatoMenuDTO;
import java.util.List;

/** Servicio del menú de La Casa JoJo. */
public interface MenuService {
    List<PlatoMenuDTO> obtenerMenu();

    PlatoMenuDTO obtenerPorId(Long id);
}
