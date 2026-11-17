package cl.elvolcan.inventario.config;

import cl.elvolcan.inventario.model.Inventario;
import cl.elvolcan.inventario.repository.InventarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final InventarioRepository inventarioRepository;

    @Override
    public void run(String... args) throws Exception {

        if (inventarioRepository.count() > 0) {
            System.out.println("Inventario ya tiene datos, omitiendo inicializacion.");
            return;
        }

        System.out.println("Inventario vacio. Cargando stock base para El Volcan...");

        crearInventario(inventarioRepository, "GAS-005", 150.0, 20.0, 500.0);
        crearInventario(inventarioRepository, "GAS-011", 200.0, 30.0, 600.0);
        crearInventario(inventarioRepository, "GAS-015", 80.0, 15.0, 300.0);
        crearInventario(inventarioRepository, "GAS-045", 40.0, 10.0, 150.0);
        crearInventario(inventarioRepository, "GAS-GRANEL", 500.0, 50.0, 2000.0);
        crearInventario(inventarioRepository, "GAS-VALVULA", 100.0, 15.0, 300.0);
        crearInventario(inventarioRepository, "GAS-REGULADOR", 80.0, 15.0, 250.0);
        crearInventario(inventarioRepository, "GAS-MANGUERA", 90.0, 15.0, 250.0);
        crearInventario(inventarioRepository, "GAS-ABRAZADERA", 150.0, 25.0, 400.0);
        crearInventario(inventarioRepository, "GAS-DETECTOR", 60.0, 10.0, 150.0);
        crearInventario(inventarioRepository, "GAS-EXTINTOR", 70.0, 10.0, 150.0);
        crearInventario(inventarioRepository, "GAS-KIT", 80.0, 15.0, 200.0);
        crearInventario(inventarioRepository, "GAS-CARGA", 999.0, 50.0, 5000.0);
        crearInventario(inventarioRepository, "GAS-DESPACHO", 999.0, 50.0, 5000.0);

        System.out.println("Inventario inicializado con exito con 14 registros.");
    }

    private void crearInventario(InventarioRepository repo, String sku, Double stockActual, Double nivelCritico, Double nivelMaximo) {
        if (repo.existsBySku(sku)) return;
        Inventario inv = new Inventario();
        inv.setSku(sku);
        inv.setStockActual(stockActual);
        inv.setNivelCritico(nivelCritico);
        inv.setNivelMaximo(nivelMaximo);
        repo.save(inv);
    }
}
