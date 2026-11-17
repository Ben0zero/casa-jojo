package cl.elvolcan.catalogo.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import cl.elvolcan.catalogo.dto.ProductoRequestDTO;
import cl.elvolcan.catalogo.dto.ProductoResponseDTO;
import cl.elvolcan.catalogo.dto.InventarioInitDTO; // AsegÃºrate de este import
import cl.elvolcan.catalogo.model.Producto;
import cl.elvolcan.catalogo.repository.ProductoRepository;
import cl.elvolcan.catalogo.service.ProductoService;
import cl.elvolcan.catalogo.client.InventarioClient;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository productoRepository;
    private final InventarioClient inventarioClient;

    @Override
    public ProductoResponseDTO crearProducto(ProductoRequestDTO request) {

        // 1. Validar si el SKU ya existe en CatÃ¡logo
        if (productoRepository.existsBySku(request.getSku())) {
            throw new RuntimeException("Error: Ya existe un producto con el SKU: " + request.getSku());
        }

        // 2. Mapear de DTO a entidad Producto
        Producto producto = new Producto();
        producto.setSku(request.getSku());
        producto.setNombre(request.getNombre());
        producto.setDescripcion(request.getDescripcion());
        producto.setCategoria(request.getCategoria());
        producto.setActivo(true);

        Integer pRes = request.getPrecioResidencial() != null ? request.getPrecioResidencial() : request.getPrecio();
        Integer pCom = request.getPrecioComercial() != null ? request.getPrecioComercial() : pRes;
        producto.setPrecioResidencial(pRes);
        producto.setPrecioComercial(pCom);
        producto.setPrecio(pRes);

        // 3. Guardar en la BD de CatÃ¡logo
        Producto guardado = productoRepository.save(producto);

        // 4. TraducciÃ³n: Preparar el objeto que Inventario espera
        InventarioInitDTO invDto = new InventarioInitDTO();
        invDto.setSku(guardado.getSku());
        invDto.setStockActual(0.0);
        invDto.setNivelCritico(10.0);
        invDto.setNivelMaximo(100.0);

        // 5. Llamar a Inventario vÃ­a Feign (el Interceptor inyectarÃ¡ el token automÃ¡ticamente)
        inventarioClient.crearInventario(invDto);

        // 6. Retornar la respuesta
        return mapearADTO(guardado);
    }

    @Override
    public List<ProductoResponseDTO> obtenerTodosActivos() {
        return productoRepository.findByActivoTrue().stream()
                .map(this::mapearADTO)
                .collect(Collectors.toList());
    }

    @Override
    public ProductoResponseDTO obtenerPorSku(String sku) {
        Producto producto = productoRepository.findBySku(sku)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con SKU: " + sku));
        return mapearADTO(producto);
    }

    @Override
    public void desactivarProducto(String sku) {
        Producto producto = productoRepository.findBySku(sku)
                .orElseThrow(() -> new RuntimeException("No se puede desactivar un SKU inexistente"));
        producto.setActivo(false);
        productoRepository.save(producto);
    }

    @Override
    public ProductoResponseDTO obtenerPorId(Long id) {
        Producto producto = productoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + id));
        return mapearADTO(producto);
    }

    // MÃ©todo auxiliar para transformar entidad -> dto
    private ProductoResponseDTO mapearADTO(Producto p) {
        ProductoResponseDTO dto = new ProductoResponseDTO();
        dto.setId(p.getId());
        dto.setSku(p.getSku());
        dto.setNombre(p.getNombre());
        dto.setDescripcion(p.getDescripcion());
        Integer pRes = p.getPrecioResidencial() != null ? p.getPrecioResidencial() : p.getPrecio();
        Integer pCom = p.getPrecioComercial() != null ? p.getPrecioComercial() : pRes;
        dto.setPrecioResidencial(pRes);
        dto.setPrecioComercial(pCom);
        dto.setPrecio(p != null && p.getPrecio() != null ? p.getPrecio() : pRes);
        dto.setCategoria(p.getCategoria());
        dto.setActivo(p.getActivo());
        return dto;
    }
}
