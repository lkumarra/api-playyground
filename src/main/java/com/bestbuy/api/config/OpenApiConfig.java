package com.bestbuy.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:3030}")
    private int serverPort;

    @Bean
    public OpenAPI apiPlaygroundOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Best Buy API Playground")
                .version("1.1.0")
                .description("""
                    A RESTful API training tool with realistic e-commerce data including \
                    51,000+ products, 1,500+ stores, and 4,300+ categories.

                    ## Pagination
                    All list endpoints return paginated results. Use `$limit` (default 10, max 25) \
                    and `$skip` (default 0) query parameters.

                    ## Filtering
                    - Products can be filtered by `category.id` or `category.name`
                    - Stores can be filtered by `service.id` or `service.name`
                    - Stores support geolocation search with `near` (zip code) and `miles` (radius)

                    ## Read-only Mode
                    When configured with `app.readonly=true`, all write operations return 405.""")
                .license(new License()
                    .name("MIT")
                    .url("https://opensource.org/licenses/MIT"))
                .contact(new Contact()
                    .name("API Playground")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:" + serverPort)
                    .description("Local development server")))
            .tags(List.of(
                new Tag().name("Products").description("CRUD operations for products. Supports filtering by category."),
                new Tag().name("Stores").description("CRUD operations for stores. Supports geolocation search and filtering by service."),
                new Tag().name("Categories").description("CRUD operations for categories. Returns subcategories and category path hierarchy."),
                new Tag().name("Services").description("CRUD operations for in-store services."),
                new Tag().name("Utilities").description("Version and health check endpoints.")));
    }
}
