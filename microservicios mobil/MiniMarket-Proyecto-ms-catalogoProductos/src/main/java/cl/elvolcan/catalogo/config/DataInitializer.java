package cl.elvolcan.catalogo.config;

import cl.elvolcan.catalogo.model.Producto;
import cl.elvolcan.catalogo.repository.ProductoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initCatalog(ProductoRepository productoRepository) {
        return args -> {

            if (productoRepository.count() > 0) {
                System.out.println("Catalogo ya tiene datos, omitiendo inicializacion.");
                return;
            }

            System.out.println("Catalogo vacio. Cargando productos de gas para El Volcan...");

            crearProducto(productoRepository, "GAS-005", "Cilindro de Gas 5kg", "Cilindro de gas licuado de petroleo 5 kg - Ideal para hogares", 15990, 14990, "Gas");
            crearProducto(productoRepository, "GAS-011", "Cilindro de Gas 11kg", "Cilindro de gas licuado de petroleo 11 kg - Uso residencial/comercial", 29990, 27990, "Gas");
            crearProducto(productoRepository, "GAS-015", "Cilindro de Gas 15kg", "Cilindro de gas licuado de petroleo 15 kg - Mayor autonomia", 39990, 37990, "Gas");
            crearProducto(productoRepository, "GAS-045", "Cilindro de Gas 45kg", "Cilindro de gas licuado de petroleo 45 kg - Uso comercial/industrial", 114990, 109990, "Gas");
            crearProducto(productoRepository, "GAS-GRANEL", "Gas Granel (m3)", "Servicio de gas licuado a granel por metro cubico", 8500, 7990, "Gas");
            crearProducto(productoRepository, "GAS-VALVULA", "Valvula para Cilindro", "Valvula de repuesto para cilindros de gas", 8990, 8490, "Accesorios");
            crearProducto(productoRepository, "GAS-REGULADOR", "Regulador de Presion", "Regulador de presion con manguera para cocina", 12990, 11990, "Accesorios");
            crearProducto(productoRepository, "GAS-MANGUERA", "Manguera de Gas", "Manguera reforzada certificada para gas", 6990, 6490, "Accesorios");
            crearProducto(productoRepository, "GAS-ABRAZADERA", "Abrazadera de Seguridad", "Juego de abrazaderas para instalacion de gas", 2990, 2790, "Accesorios");
            crearProducto(productoRepository, "GAS-DETECTOR", "Detector de Gas", "Detector de fugas de gas con alarma", 24990, 22990, "Seguridad");
            crearProducto(productoRepository, "GAS-EXTINTOR", "Extintor ABC 2kg", "Extintor multiproposito para uso domiciliario/comercial", 19990, 18990, "Seguridad");
            crearProducto(productoRepository, "GAS-KIT", "Kit de Instalacion Basico", "Kit basico para instalacion de cilindro de gas", 15990, 14990, "Accesorios");
            crearProducto(productoRepository, "GAS-CARGA", "Carga de Gas 11kg (Servicio)", "Servicio de recarga/cambio de cilindro 11kg", 28990, 26990, "Servicios");
            crearProducto(productoRepository, "GAS-DESPACHO", "Despacho a Domicilio", "Servicio de despacho y entrega a domicilio", 3990, 2990, "Servicios");

            System.out.println("Catalogo inicializado con exito con 14 productos de gas.");
        };
    }

    private void crearProducto(ProductoRepository repository, String sku, String nombre, String descripcion, int precioRes, int precioCom, String categoria) {
        if (repository.existsBySku(sku)) return;
        Producto nuevo = new Producto();
        nuevo.setSku(sku);
        nuevo.setNombre(nombre);
        nuevo.setDescripcion(descripcion);
        nuevo.setPrecioResidencial(precioRes);
        nuevo.setPrecioComercial(precioCom);
        nuevo.setPrecio(precioRes);
        nuevo.setCategoria(categoria);
        nuevo.setActivo(true);
        repository.save(nuevo);
    }
}
