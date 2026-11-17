package cl.elvolcan.ventas.client;

import cl.elvolcan.ventas.dto.ProductoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "ms-catalogoProductos")
public interface CatalogoClient {

    @GetMapping("/api/productos/id/{id}")
    ProductoDTO obtenerProductoPorId(@RequestHeader("Authorization") String token, @PathVariable("id") Long id);

    @GetMapping("/api/productos/{sku}")
    ProductoDTO obtenerProductoPorSku(@RequestHeader("Authorization") String token, @PathVariable("sku") String sku);
}
