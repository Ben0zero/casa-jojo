package cl.casajojo.catalogo.service.impl;

import cl.casajojo.catalogo.dto.PlatoMenuDTO;
import cl.casajojo.catalogo.exception.PlatoNoEncontradoException;
import cl.casajojo.catalogo.model.Producto;
import cl.casajojo.catalogo.repository.ProductoRepository;
import cl.casajojo.catalogo.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Traduce la tabla PRODUCTOS (compartida en Oracle con otros esquemas) al
 * menú de Casa JoJo. Solo se exponen los SKU con prefijo "CJ-".
 */
@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private static final String PREFIJO_CJ = "CJ-";

    private final ProductoRepository productoRepository;

    @Override
    public List<PlatoMenuDTO> obtenerMenu() {
        return productoRepository.findAll().stream()
                .filter(p -> p.getSku() != null && p.getSku().startsWith(PREFIJO_CJ))
                .map(this::mapear)
                .collect(Collectors.toList());
    }

    @Override
    public PlatoMenuDTO obtenerPorId(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new PlatoNoEncontradoException("Plato no encontrado: " + id)); // B1: 404
        if (producto.getSku() == null || !producto.getSku().startsWith(PREFIJO_CJ)) {
            throw new PlatoNoEncontradoException("El producto " + id + " no pertenece al menú Casa JoJo"); // B1: 404
        }
        return mapear(producto);
    }

    @Override
    public PlatoMenuDTO cambiarDisponibilidad(Long id, boolean disponible) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new PlatoNoEncontradoException("Plato no encontrado: " + id));
        if (producto.getSku() == null || !producto.getSku().startsWith(PREFIJO_CJ)) {
            throw new PlatoNoEncontradoException("El producto " + id + " no pertenece al menú Casa JoJo");
        }
        // "activo" es la columna heredada que el menú expone como "disponible".
        producto.setActivo(disponible);
        return mapear(productoRepository.save(producto));
    }

    private PlatoMenuDTO mapear(Producto p) {
        String categoria = p.getCategoria() == null ? "" : p.getCategoria().toUpperCase(Locale.ROOT);
        String tipo = categoria.startsWith("ACOM") ? "acompanamiento" : "proteina";
        Integer precio = p.getPrecio() != null ? p.getPrecio() :
                (p.getPrecioResidencial() != null ? p.getPrecioResidencial() : 0);
        boolean disponible = !Boolean.FALSE.equals(p.getActivo());
        return new PlatoMenuDTO(
                p.getId(), p.getSku(), p.getNombre(), p.getDescripcion(),
                precio, tipo, disponible
        );
    }
}
