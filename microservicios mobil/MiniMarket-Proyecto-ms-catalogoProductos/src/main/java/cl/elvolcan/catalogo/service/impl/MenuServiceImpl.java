package cl.elvolcan.catalogo.service.impl;

import cl.elvolcan.catalogo.dto.PlatoMenuDTO;
import cl.elvolcan.catalogo.model.Producto;
import cl.elvolcan.catalogo.repository.ProductoRepository;
import cl.elvolcan.catalogo.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Traduce el catálogo (tabla PRODUCTOS, compartida con el proyecto de gas) al
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
                .orElseThrow(() -> new IllegalArgumentException("Plato no encontrado: " + id));
        if (producto.getSku() == null || !producto.getSku().startsWith(PREFIJO_CJ)) {
            throw new IllegalArgumentException("El producto " + id + " no pertenece al menú Casa JoJo");
        }
        return mapear(producto);
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
