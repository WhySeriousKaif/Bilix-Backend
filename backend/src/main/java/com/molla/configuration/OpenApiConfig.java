package com.molla.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 📚 OpenAPI/Swagger Configuration
 * 
 * 👉 Purpose: Configure Swagger UI for API documentation and testing
 * 🔹 Why: Provides interactive API documentation (perfect for viva demo)
 * 
 * 📌 Access: http://localhost:5001/swagger-ui/index.html
 * 🔹 Features: Try endpoints, see request/response schemas, test with JWT tokens
 */
@Configuration
public class OpenApiConfig {

    /**
     * 📖 OpenAPI Bean - Swagger documentation configuration
     * 
     * 👉 Purpose: Define API metadata (title, version, description, server URL)
     * 🔹 Flow: Spring Boot auto-detects this bean → Generates Swagger UI
     */
    @Bean
    public OpenAPI mollaPosOpenAPI() {
        // 🌐 Server Configuration - Where API is hosted
        Server localServer = new Server();
        localServer.setUrl("http://localhost:5001");  // Local development URL
        localServer.setDescription("Local Development Server");

        // 📝 API Information - Metadata shown in Swagger UI
        Info info = new Info()
                .title("Bilix POS System API Documentation")
                .version("1.0.0")
                .description("Complete API documentation for Bilix POS System. " +
                        "Includes Authentication, Products, Orders, Inventory, Analytics, and more. " +
                        "\n\n**How to use:**\n" +
                        "1. First, call `/auth/login` or `/auth/register` to get a JWT token\n" +
                        "2. Click the **Authorize** button (🔒) at the top right\n" +
                        "3. Paste your JWT token (format: `Bearer <your-token>` or just `<your-token>`)\n" +
                        "4. Now you can test all protected endpoints!")
                .contact(new Contact()
                        .name("Bilix POS System")
                        .email("support@molla.com"))
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"));

        // 🔐 JWT Security Scheme - Enables "Authorize" button in Swagger UI
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Enter your JWT token. You can get it by calling /auth/login endpoint.");

        // 🔨 Build and return OpenAPI configuration
        return new OpenAPI()
                .info(info)  // Set API metadata
                .servers(List.of(localServer))  // Set server URLs
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))  // Apply security globally
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("bearerAuth", securityScheme));  // Register JWT security scheme
    }
}
