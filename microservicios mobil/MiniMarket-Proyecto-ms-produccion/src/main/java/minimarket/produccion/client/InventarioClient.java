package minimarket.produccion.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "inventario-service", 
    url = "http://localhost:8084"
)
public interface InventarioClient {

    @PutMapping("/api/inventario/sumar-stock/{sku}/{cantidad}")
    String sumarStock(
            @PathVariable String sku, 
            @PathVariable int cantidad
    );
}