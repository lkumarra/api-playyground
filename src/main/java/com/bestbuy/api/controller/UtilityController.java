package com.bestbuy.api.controller;

import com.bestbuy.api.repository.CategoryRepository;
import com.bestbuy.api.repository.ProductRepository;
import com.bestbuy.api.repository.StoreRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.util.Map;

@RestController
@Tag(name = "Utilities")
public class UtilityController {

    @Value("${app.version}")
    private String version;

    @Value("${app.readonly}")
    private boolean readonly;

    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final CategoryRepository categoryRepository;

    public UtilityController(ProductRepository productRepository,
                             StoreRepository storeRepository,
                             CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.storeRepository = storeRepository;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping("/version")
    @Operation(
        summary = "Get API version",
        description = "Returns the current API version.",
        responses = {
            @ApiResponse(responseCode = "200", description = "API version",
                content = @Content(examples = @ExampleObject(value = """
                    { "version": "1.1.0" }""")))
        }
    )
    public Map<String, String> version() {
        return Map.of("version", version);
    }

    @GetMapping("/healthcheck")
    @Operation(
        summary = "Get system health information",
        description = "Returns uptime in seconds, read-only mode status, and document counts for products, stores, and categories.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Health check result",
                content = @Content(examples = @ExampleObject(value = """
                    {
                      "uptime": 123.456,
                      "readonly": false,
                      "documents": {
                        "products": 51957,
                        "stores": 1561,
                        "categories": 4307
                      }
                    }""")))
        }
    )
    public Map<String, Object> healthcheck() {
        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        double uptimeSeconds = uptimeMs / 1000.0;

        return Map.of(
            "uptime", uptimeSeconds,
            "readonly", readonly,
            "documents", Map.of(
                "products", productRepository.count(),
                "stores", storeRepository.count(),
                "categories", categoryRepository.count()
            )
        );
    }
}
