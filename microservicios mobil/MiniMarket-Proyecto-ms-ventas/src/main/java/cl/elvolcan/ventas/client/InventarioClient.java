package cl.elvolcan.ventas.client;

import cl.elvolcan.ventas.dto.StockResponseDTO;
import cl.elvolcan.ventas.dto.StockRequestDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "ms-inventario")
public interface InventarioClient {

    @GetMapping("/api/inventario/{sku}")
    StockResponseDTO obtenerStock(@RequestHeader("Authorization") String token, @PathVariable("sku") String sku);

    @PutMapping("/api/inventario/disminuir")
    void disminuirStock(@RequestHeader("Authorization") String token, @RequestBody StockRequestDTO request);
}
