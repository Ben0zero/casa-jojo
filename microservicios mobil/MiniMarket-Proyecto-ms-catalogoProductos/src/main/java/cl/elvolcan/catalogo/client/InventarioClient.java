package cl.elvolcan.catalogo.client;

import cl.elvolcan.catalogo.dto.InventarioInitDTO; //  Usaremos este nuevo DTO
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "inventario-service", url = "http://localhost:8084/")
public interface InventarioClient {

        @PostMapping("/api/inventario/crear")
        Object crearInventario(@RequestBody InventarioInitDTO dto); //  Cambiamos el DTO
}