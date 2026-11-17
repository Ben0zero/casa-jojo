package cl.elvolcan.catalogo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class CatalogoproductosApplication {

	public static void main(String[] args) {
		SpringApplication.run(CatalogoproductosApplication.class, args);
	}

}
