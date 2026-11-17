package cl.elvolcan.inventario.service;

import cl.elvolcan.inventario.dto.request.StockRequestDTO;
import cl.elvolcan.inventario.dto.response.StockResponseDTO;

import java.util.List;

public interface InventarioService {

    // Crear nuevo producto en inventario
    StockResponseDTO crearProducto(StockRequestDTO request);

    // Aumentar stock
    StockResponseDTO aumentarStock(StockRequestDTO request);

    // Disminuir stock (ventas)
    StockResponseDTO disminuirStock(StockRequestDTO request);

    // Obtener stock por SKU
    StockResponseDTO obtenerPorSku(String sku);

    // Obtener productos en stock critico
    List<StockResponseDTO> obtenerStockCritico();
    
    // Conectar a Produccion
    String sumarStock(String sku, int cantidad);

    // Obtener todo el inventario
    List<StockResponseDTO> obtenerTodos();
}
