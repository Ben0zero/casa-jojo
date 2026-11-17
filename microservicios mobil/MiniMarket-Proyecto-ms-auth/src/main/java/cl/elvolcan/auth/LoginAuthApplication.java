package cl.elvolcan.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class LoginAuthApplication {

	public static void main(String[] args) {
		SpringApplication.run(LoginAuthApplication.class, args);
	}

}
