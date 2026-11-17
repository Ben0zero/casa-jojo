package cl.elvolcan.catalogo.config;

import cl.elvolcan.catalogo.model.Producto;
import cl.elvolcan.catalogo.repository.ProductoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Siembra el menú de La Casa JoJo en la MISMA tabla de productos (SKU CJ-*),
 * sin borrar el catálogo heredado del proyecto de gas. Es idempotente.
 */
@Configuration
public class MenuInitializer {

    @Bean
    CommandLineRunner initMenuCasaJojo(ProductoRepository productoRepository) {
        return args -> {
            crear(productoRepository, "CJ-PROT-01", "Pollo asado", "Pollo asado a la parrilla", 4800, "PROTEINA", true);
            crear(productoRepository, "CJ-PROT-02", "Carne al jugo", "Carne al jugo con su salsa", 4800, "PROTEINA", true);
            crear(productoRepository, "CJ-PROT-03", "Guatitas", "Guatitas estofadas", 4600, "PROTEINA", true);
            crear(productoRepository, "CJ-PROT-04", "Cerdo mongoliano", "Cerdo mongoliano (agotado)", 4600, "PROTEINA", false);
            crear(productoRepository, "CJ-PROT-05", "Filete de pollo", "Filete de pollo a la plancha", 4600, "PROTEINA", true);
            crear(productoRepository, "CJ-PROT-06", "Tortilla de porotos verdes", "Tortilla de porotos verdes", 4600, "PROTEINA", true);

            crear(productoRepository, "CJ-ACOM-01", "Arroz", "Arroz blanco", 0, "ACOMPANAMIENTO", true);
            crear(productoRepository, "CJ-ACOM-02", "Arroz integral", "Arroz integral", 0, "ACOMPANAMIENTO", true);
            crear(productoRepository, "CJ-ACOM-03", "Verduras salteadas", "Verduras de estación salteadas", 0, "ACOMPANAMIENTO", true);
            crear(productoRepository, "CJ-ACOM-04", "Fideos blancos", "Fideos blancos con salsa", 0, "ACOMPANAMIENTO", true);
            crear(productoRepository, "CJ-ACOM-05", "Fideos al pesto", "Fideos con salsa pesto", 0, "ACOMPANAMIENTO", true);
            crear(productoRepository, "CJ-ACOM-06", "Papas mayo", "Papas mayo caseras", 0, "ACOMPANAMIENTO", true);
            crear(productoRepository, "CJ-ACOM-07", "Puré", "Puré de papas", 0, "ACOMPANAMIENTO", true);
            crear(productoRepository, "CJ-ACOM-08", "Ensalada", "Ensalada fresca", 0, "ACOMPANAMIENTO", true);
        };
    }

    private void crear(ProductoRepository repo, String sku, String nombre, String descripcion,
                       int precio, String categoria, boolean activo) {
        if (repo.existsBySku(sku)) {
            return;
        }
        Producto p = new Producto();
        p.setSku(sku);
        p.setNombre(nombre);
        p.setDescripcion(descripcion);
        p.setPrecio(precio);
        p.setPrecioResidencial(precio);
        p.setPrecioComercial(precio);
        p.setCategoria(categoria);
        p.setActivo(activo);
        repo.save(p);
    }
}
