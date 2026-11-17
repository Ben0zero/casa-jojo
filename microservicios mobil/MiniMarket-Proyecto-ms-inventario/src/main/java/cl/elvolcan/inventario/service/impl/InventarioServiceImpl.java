package cl.elvolcan.inventario.service.impl;

import cl.elvolcan.inventario.dto.request.StockRequestDTO;
import cl.elvolcan.inventario.dto.response.StockResponseDTO;
import cl.elvolcan.inventario.exception.*;
import cl.elvolcan.inventario.model.Inventario;
import cl.elvolcan.inventario.repository.InventarioRepository;
import cl.elvolcan.inventario.service.InventarioService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class InventarioServiceImpl implements InventarioService {

    private static final Logger logger = LoggerFactory.getLogger(InventarioServiceImpl.class);

    @Autowired
    private InventarioRepository repository;

    // Crear producto
    @Override
    public StockResponseDTO crearProducto(StockRequestDTO request) {

        if (repository.existsBySku(request.getSku())) {
            throw new RuntimeException("El producto ya existe");
        }

        Inventario inventario = new Inventario();
        inventario.setSku(request.getSku());
        
        // Aqui usamos los nuevos campos del DTO que configuramos
        inventario.setStockActual(request.getStockActual());
        inventario.setNivelCritico(request.getNivelCritico()); 
        inventario.setNivelMaximo(request.getNivelMaximo());

        repository.save(inventario);

        logger.info("Producto creado: {}", request.getSku());

        return mapToResponse(inventario);
    }

    // Aumentar stock
    @Override
    public StockResponseDTO aumentarStock(StockRequestDTO request) {

        Inventario inventario = repository.findBySku(request.getSku())
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

        // Cambio getCantidad() por getStockActual()
        double nuevoStock = inventario.getStockActual() + request.getStockActual();

        if (nuevoStock > inventario.getNivelMaximo()) {
            throw new OverstockException("Se supera el stock maximo permitido");
        }

        inventario.setStockActual(nuevoStock);

        repository.save(inventario);

        logger.info("Stock aumentado - SKU: {} | Nuevo stock: {}", request.getSku(), nuevoStock);

        return mapToResponse(inventario);
    }

    // Disminuir stock
    @Override
    public StockResponseDTO disminuirStock(StockRequestDTO request) {

        Inventario inventario = repository.findBySku(request.getSku())
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

        // Cambio getCantidad() por getStockActual()
        if (inventario.getStockActual() < request.getStockActual()) {
            throw new InsufficientStockException("Stock insuficiente");
        }

        double nuevoStock = inventario.getStockActual() - request.getStockActual();
        inventario.setStockActual(nuevoStock);

        if (nuevoStock <= inventario.getNivelCritico()) {
            logger.warn("STOCK CRITICO - SKU: {} | Stock: {}", request.getSku(), nuevoStock);
        }

        repository.save(inventario);

        return mapToResponse(inventario);
    }

    // Obtener por SKU
    @Override
    public StockResponseDTO obtenerPorSku(String sku) {

        Inventario inventario = repository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

        return mapToResponse(inventario);
    }

    // Obtener stock critico
    @Override
    public List<StockResponseDTO> obtenerStockCritico() {

        return repository.findStockCritico()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Conectar con Produccion
    @Override
    public String sumarStock(String sku, int cantidad) {

        Inventario inventario = repository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

        double nuevoStock = inventario.getStockActual() + cantidad;

        inventario.setStockActual(nuevoStock);

        repository.save(inventario);

        logger.info("Stock sumado desde produccion - SKU: {} | Nuevo stock: {}", sku, nuevoStock);

        return "Stock actualizado correctamente";
    }

    // Obtener todos los productos del inventario
    @Override
    public List<StockResponseDTO> obtenerTodos() {

        return repository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Mapper
    private StockResponseDTO mapToResponse(Inventario inventario) {

        boolean bajoStock = inventario.getStockActual() <= inventario.getNivelCritico();

        return new StockResponseDTO(
                inventario.getSku(),
                inventario.getStockActual(),
                bajoStock
        );
    }
}