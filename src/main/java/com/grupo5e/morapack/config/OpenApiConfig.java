package com.grupo5e.morapack.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuración de OpenAPI/Swagger para documentación de la API.
 * Define información del proyecto, servidores y configuraciones de Swagger UI.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Configura la información de la API para Swagger/OpenAPI.
     * Define título, descripción, versión, contacto y servidores disponibles.
     */
    @Bean
    public OpenAPI moraPackOpenAPI() {
        Server localServer = new Server();
        localServer.setUrl("http://localhost:8080");
        localServer.setDescription("Servidor local de desarrollo");

        Contact contact = new Contact();
        contact.setName("Grupo 5E");
        contact.setEmail("grupo5e@morapack.com");

        License mitLicense = new License()
                .name("MIT License")
                .url("https://choosealicense.com/licenses/mit/");

        Info info = new Info()
                .title("MoraPack API")
                .version("1.0.0")
                .contact(contact)
                .description("API REST para el sistema de gestión de paquetería MoraPack. " +
                        "Incluye gestión completa de pedidos, clientes, vuelos, rutas y algoritmo de optimización ALNS.")
                .termsOfService("https://morapack.com/terms")
                .license(mitLicense);

        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer));
    }
}

